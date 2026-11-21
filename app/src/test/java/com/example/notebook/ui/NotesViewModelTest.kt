package com.example.notebook.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.notebook.data.FakeClock
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.inMemoryDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.CategoryResult
import com.example.notebook.ui.screens.notes.NotesUiState
import com.example.notebook.ui.screens.notes.NotesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Category filter on the main screen (docs/SPEC.md, "Категории"). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NotesViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var db: NotebookDatabase
    private lateinit var notes: NoteRepository
    private lateinit var categories: CategoryRepository
    private lateinit var scope: CoroutineScope
    private lateinit var vm: NotesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDatabase()
        val clock = FakeClock()
        notes = NoteRepository(db.noteDao(), clock.function)
        categories = CategoryRepository(db, clock.function)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = folder.newFile("s.preferences_pb").also { it.delete() }
        val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        vm = NotesViewModel(notes, categories, settings, UserMessenger(), scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
        Dispatchers.resetMain()
    }

    /** Waits (in real time, Room runs on its own threads) until the state matches. */
    private suspend fun stateWhere(predicate: (NotesUiState) -> Boolean): NotesUiState =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { vm.uiState.first { !it.isLoading && predicate(it) } }
        }

    private fun NotesUiState.titles() = (pinned + others).map { it.note.title }

    @Test
    fun filterShowsOnlyNotesOfThatCategory() = runTest {
        val work = (categories.createCategory("Работа") as CategoryResult.Success).id
        notes.createNote("отчёт", "", categoryId = work)
        notes.createNote("хлеб", "")

        assertEquals(setOf("отчёт", "хлеб"), stateWhere { it.titles().size == 2 }.titles().toSet())

        vm.setFilter(work)
        val filtered = stateWhere { it.filterCategoryId == work }
        assertEquals(listOf("отчёт"), filtered.titles())

        vm.setFilter(null)
        assertEquals(2, stateWhere { it.filterCategoryId == null }.titles().size)
    }

    @Test
    fun emptyCategoryShowsEmptyFilteredState() = runTest {
        val empty = (categories.createCategory("Пусто") as CategoryResult.Success).id
        notes.createNote("a", "")
        vm.setFilter(empty)
        val state = stateWhere { it.filterCategoryId == empty }
        assertTrue(state.isEmpty)
    }

    @Test
    fun deletingFilteredCategoryFallsBackToAll() = runTest {
        val temp = (categories.createCategory("Временная") as CategoryResult.Success).id
        notes.createNote("a", "", categoryId = temp)
        vm.setFilter(temp)
        stateWhere { it.filterCategoryId == temp }

        categories.deleteCategory(temp)
        val state = stateWhere { it.categories.isEmpty() }
        assertEquals(null, state.filterCategoryId)
        assertEquals(listOf("a"), state.titles())
    }
}
