package com.example.notebook.data

import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.domain.CategoryResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Category rules from docs/SPEC.md, "Категории". */
@RunWith(RobolectricTestRunner::class)
class CategoryRepositoryTest {

    private lateinit var db: NotebookDatabase
    private lateinit var categories: CategoryRepository
    private lateinit var notes: NoteRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val clock = FakeClock()
        categories = CategoryRepository(db, clock.function)
        notes = NoteRepository(db.noteDao(), clock.function)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun create(name: String) = categories.createCategory(name)
    private suspend fun idOf(name: String) = (create(name) as CategoryResult.Success).id

    @Test
    fun createTrimsName() = runTest {
        idOf("  Работа  ")
        assertEquals(listOf("Работа"), categories.observeCategories().first().map { it.name })
    }

    @Test
    fun duplicateIgnoringCaseIsRejected_Latin() = runTest {
        idOf("Work")
        assertEquals(CategoryResult.DuplicateName, create("work"))
        assertEquals(CategoryResult.DuplicateName, create("WORK "))
    }

    @Test
    fun duplicateIgnoringCaseIsRejected_Cyrillic() = runTest {
        idOf("Работа")
        assertEquals(CategoryResult.DuplicateName, create("работа"))
        assertEquals(CategoryResult.DuplicateName, create("РАБОТА"))
    }

    @Test
    fun emptyAndTooLongNamesAreRejected() = runTest {
        assertEquals(CategoryResult.EmptyName, create("   "))
        assertEquals(CategoryResult.TooLong, create("я".repeat(31)))
        assertTrue(create("я".repeat(30)) is CategoryResult.Success)
    }

    @Test
    fun renameChecksDuplicatesButAllowsChangingOwnCase() = runTest {
        val home = idOf("Дом")
        idOf("Работа")
        assertEquals(CategoryResult.DuplicateName, categories.renameCategory(home, "работа"))
        assertEquals(CategoryResult.Success(home), categories.renameCategory(home, "ДОМ"))
        assertEquals(CategoryResult.NotFound, categories.renameCategory(999, "Новое"))
        assertEquals(setOf("ДОМ", "Работа"), categories.observeCategories().first().map { it.name }.toSet())
    }

    @Test
    fun categoriesAreSortedAlphabetically() = runTest {
        idOf("яблоки")
        idOf("Арбуз")
        idOf("банан")
        assertEquals(listOf("Арбуз", "банан", "яблоки"), categories.observeCategories().first().map { it.name })
    }

    @Test
    fun noteCountIncludesOnlyMainScreenNotes() = runTest {
        val cat = idOf("Работа")
        val visible = notes.createNote("a", "", categoryId = cat)
        val archived = notes.createNote("b", "", categoryId = cat)
        val trashed = notes.createNote("c", "", categoryId = cat)
        notes.archive(listOf(archived))
        notes.moveToTrash(listOf(trashed))

        assertEquals(1, categories.observeCategories().first().single().noteCount)
        // The delete dialog counts every note that would lose the category.
        assertEquals(3, categories.countNotesInCategory(cat))
        assertTrue(visible > 0)
    }
}
