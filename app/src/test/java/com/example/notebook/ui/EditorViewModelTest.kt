package com.example.notebook.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.notebook.data.FakeClock
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.inMemoryDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.domain.CategoryResult
import com.example.notebook.domain.NoteColor
import com.example.notebook.domain.SortOrder
import com.example.notebook.ui.screens.editor.EditorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Autosave rules from docs/SPEC.md, "Создание и автосохранение". */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var db: NotebookDatabase
    private lateinit var notes: NoteRepository
    private lateinit var categories: CategoryRepository
    private lateinit var messenger: UserMessenger
    private lateinit var appScope: CoroutineScope
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = inMemoryDatabase()
        notes = NoteRepository(db.noteDao(), FakeClock().function)
        categories = CategoryRepository(db)
        messenger = UserMessenger()
        appScope = CoroutineScope(SupervisorJob() + dispatcher)
    }

    @After
    fun tearDown() {
        appScope.cancel()
        db.close()
        Dispatchers.resetMain()
    }

    private fun editor(noteId: Long? = null, categoryId: Long? = null): EditorViewModel {
        val args = buildMap<String, Any> {
            noteId?.let { put("noteId", it) }
            categoryId?.let { put("categoryId", it) }
        }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditorViewModel(SavedStateHandle(args), notes, categories, messenger, appScope) as T
        }
        return ViewModelProvider(store, factory)[EditorViewModel::class.java]
    }

    /** Closing the editor = clearing its ViewModel, exactly like leaving the screen. */
    private fun close() = store.clear()

    /** Lets coroutines on the test dispatcher and Room's background threads finish. */
    private suspend fun TestScope.settle() {
        repeat(5) {
            dispatcher.scheduler.advanceUntilIdle()
            withContext(Dispatchers.IO) { db.noteDao().getAll() } // wait for Room's queue
        }
    }

    private suspend fun allNotes() = notes.observeActiveNotes(SortOrder.UPDATED_NEWEST_FIRST).first()

    @Test
    fun openingAndClosingEmptyEditorCreatesNothing() = runTest(dispatcher) {
        editor()
        settle()
        close()
        settle()
        assertTrue(db.noteDao().getAll().isEmpty())
    }

    @Test
    fun firstCharacterInsertsNoteImmediately() = runTest(dispatcher) {
        val vm = editor()
        settle()
        vm.onContentChange("М")
        settle()
        assertEquals(listOf("М"), allNotes().map { it.content })
    }

    @Test
    fun laterChangesAreSavedAfterPauseWithoutDuplicates() = runTest(dispatcher) {
        val vm = editor()
        settle()
        vm.onContentChange("М")
        settle()
        vm.onContentChange("Мо")
        vm.onContentChange("Мол")
        advanceTimeBy(EditorViewModel.AUTOSAVE_DELAY_MS - 100)
        assertEquals("М", allNotes().single().content) // still debouncing
        advanceTimeBy(200)
        settle()
        assertEquals("Мол", allNotes().single().content)
    }

    @Test
    fun closingSavesPendingTextWithoutWaitingForDebounce() = runTest(dispatcher) {
        val vm = editor()
        settle()
        vm.onTitleChange("Покупки")
        settle()
        vm.onContentChange("Молоко")
        close()
        settle()
        val note = allNotes().single()
        assertEquals("Покупки", note.title)
        assertEquals("Молоко", note.content)
    }

    @Test
    fun newNoteClearedBeforeExitIsDeleted() = runTest(dispatcher) {
        val vm = editor()
        settle()
        vm.onContentChange("x")
        settle()
        vm.onContentChange("   ")
        close()
        settle()
        assertTrue(db.noteDao().getAll().isEmpty())
    }

    @Test
    fun colorAndPinChosenBeforeTypingAreApplied() = runTest(dispatcher) {
        val vm = editor()
        settle()
        vm.changeColor(NoteColor.MINT)
        vm.togglePinned()
        vm.onContentChange("a")
        settle()
        val note = allNotes().single()
        assertEquals(NoteColor.MINT, note.color)
        assertTrue(note.isPinned)
    }

    @Test
    fun existingNoteLoadsAndOpeningDoesNotChangeDate() = runTest(dispatcher) {
        val id = notes.createNote("t", "c")
        val before = notes.getNote(id)!!.updatedAt
        val vm = editor(id)
        settle()
        assertEquals("c", vm.content)
        close()
        settle()
        assertEquals(before, notes.getNote(id)!!.updatedAt)
    }

    @Test
    fun newNoteFromCategoryFilterGetsThatCategory() = runTest(dispatcher) {
        val work = (categories.createCategory("Работа") as CategoryResult.Success).id
        val vm = editor(categoryId = work)
        settle()
        vm.onContentChange("отчёт")
        settle()
        assertEquals(work, allNotes().single().categoryId)
    }

    @Test
    fun changingCategoryOfExistingNoteIsSaved() = runTest(dispatcher) {
        val id = notes.createNote("t", "c")
        val vm = editor(id)
        settle()
        val result = vm.createCategoryAndAssign("Дом")
        settle()
        val home = (result as CategoryResult.Success).id
        assertEquals(home, notes.getNote(id)!!.categoryId)

        vm.changeCategory(null)
        settle()
        assertEquals(null, notes.getNote(id)!!.categoryId)
    }

    @Test
    fun categoryDeletedBeforeFirstSaveDoesNotLoseText() = runTest(dispatcher) {
        val temp = (categories.createCategory("Временная") as CategoryResult.Success).id
        val vm = editor(categoryId = temp)
        settle()
        categories.deleteCategory(temp)
        vm.onContentChange("важный текст")
        settle()
        val note = allNotes().single()
        assertEquals("важный текст", note.content)
        assertEquals(null, note.categoryId)
    }

    @Test
    fun deleteMovesToTrashAndOffersUndo() = runTest(dispatcher) {
        val id = notes.createNote("t", "c")
        val vm = editor(id)
        settle()
        vm.moveToTrash()
        settle()
        assertTrue(notes.getNote(id)!!.isInTrash)

        val message = messenger.messages.first()
        message.action!!.invoke()
        settle()
        assertEquals(listOf(id), allNotes().map { it.id })
    }
}
