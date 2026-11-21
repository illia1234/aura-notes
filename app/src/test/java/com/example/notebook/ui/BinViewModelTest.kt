package com.example.notebook.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.notebook.data.FakeClock
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.inMemoryDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.SortOrder
import com.example.notebook.ui.screens.bin.BinMode
import com.example.notebook.ui.screens.bin.BinUiState
import com.example.notebook.ui.screens.bin.BinViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Archive and trash actions (docs/SPEC.md, "Удаление, архив, корзина"). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BinViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var db: NotebookDatabase
    private lateinit var notes: NoteRepository
    private lateinit var scope: CoroutineScope
    private lateinit var messenger: UserMessenger

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = inMemoryDatabase()
        notes = NoteRepository(db.noteDao(), FakeClock().function)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        messenger = UserMessenger()
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
        Dispatchers.resetMain()
    }

    private fun bin(mode: BinMode): BinViewModel {
        val file = folder.newFile("bin-${mode.name}.preferences_pb").also { it.delete() }
        val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        return BinViewModel(mode, notes, CategoryRepository(db), settings, messenger, scope)
    }

    /** Waits in real time (Room works on its own threads) until the state matches. */
    private suspend fun BinViewModel.stateWhere(predicate: (BinUiState) -> Boolean): BinUiState =
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { uiState.first { !it.isLoading && predicate(it) } }
        }

    private suspend fun waitUntil(check: suspend () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) { while (!check()) kotlinx.coroutines.delay(20) }
    }

    private suspend fun active() = notes.observeActiveNotes(SortOrder.UPDATED_NEWEST_FIRST).first()

    @Test
    fun archiveListsOnlyArchivedNotesAndUnarchiveReturnsThem() = runTest {
        val kept = notes.createNote("на главном", "")
        val archived = notes.createNote("в архиве", "")
        notes.archive(listOf(archived))
        val vm = bin(BinMode.Archive)

        assertEquals(listOf(archived), vm.stateWhere { it.items.isNotEmpty() }.items.map { it.note.id })

        vm.toggleSelection(archived)
        vm.stateWhere { it.isSelecting }
        vm.unarchiveSelected()
        waitUntil { active().size == 2 }
        assertEquals(setOf(kept, archived), active().map { it.id }.toSet())
        assertTrue(messenger.messages.first().action != null) // "Отменить" offered
    }

    @Test
    fun restoreFromTrashBringsNoteBack() = runTest {
        val id = notes.createNote("t", "")
        notes.moveToTrash(listOf(id))
        val vm = bin(BinMode.Trash)
        vm.stateWhere { it.items.size == 1 }

        vm.toggleSelection(id)
        vm.stateWhere { it.isSelecting }
        vm.restoreSelected()
        waitUntil { active().isNotEmpty() }
        assertNull(notes.getNote(id)!!.deletedAt)
    }

    @Test
    fun deleteForeverAndEmptyTrashRemoveRows() = runTest {
        val a = notes.createNote("a", "")
        val b = notes.createNote("b", "")
        val alive = notes.createNote("alive", "")
        notes.moveToTrash(listOf(a, b))
        val vm = bin(BinMode.Trash)
        vm.stateWhere { it.items.size == 2 }

        vm.toggleSelection(a)
        vm.stateWhere { it.isSelecting }
        vm.deleteSelectedForever()
        waitUntil { notes.getNote(a) == null }
        assertTrue(notes.getNote(b)!!.isInTrash)

        vm.emptyTrash()
        waitUntil { notes.getNote(b) == null }
        assertEquals(listOf(alive), active().map { it.id })
    }
}
