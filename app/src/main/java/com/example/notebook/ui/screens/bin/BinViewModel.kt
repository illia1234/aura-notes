package com.example.notebook.ui.screens.bin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.ui.NoteMessages
import com.example.notebook.ui.UserMessenger
import com.example.notebook.ui.screens.notes.NoteItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Archive and Trash look the same (a grid without FAB), so they share one screen. */
enum class BinMode { Archive, Trash }

data class BinUiState(
    val items: List<NoteItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
    val isSelecting: Boolean get() = selectedIds.isNotEmpty()
}

/**
 * Archive or Trash list with multi-select.
 * Archive: return to the main screen, or move to the trash.
 * Trash: restore, or delete permanently (the screen confirms first), or empty the trash.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BinViewModel(
    val mode: BinMode,
    private val notes: NoteRepository,
    categories: CategoryRepository,
    settings: SettingsRepository,
    private val messenger: UserMessenger,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    private val noteList = when (mode) {
        // The archive uses the same order as the main screen; the trash shows recently deleted first.
        BinMode.Archive -> settings.sortOrder.flatMapLatest { notes.observeArchivedNotes(it) }
        BinMode.Trash -> notes.observeTrash()
    }

    val uiState: StateFlow<BinUiState> = combine(
        noteList,
        categories.observeCategories(),
        selection,
    ) { list, cats, selected ->
        val names = cats.associate { it.id to it.name }
        BinUiState(
            items = list.map { NoteItem(it, it.categoryId?.let(names::get)) },
            selectedIds = selected intersect list.mapTo(HashSet()) { it.id },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BinUiState())

    fun toggleSelection(id: Long) = selection.update { if (id in it) it - id else it + id }

    fun clearSelection() = selection.update { emptySet() }

    private fun takeSelection(): List<Long> = uiState.value.selectedIds.toList().also { clearSelection() }

    /** Archive: back to the main screen, with "Отменить". */
    fun unarchiveSelected() {
        val ids = takeSelection()
        appScope.launch {
            notes.unarchive(ids)
            messenger.send(NoteMessages.unarchived(ids.size) { notes.archive(ids) })
        }
    }

    /** Archive: to the trash, with "Отменить". */
    fun trashSelected() {
        val ids = takeSelection()
        appScope.launch {
            notes.moveToTrash(ids)
            messenger.send(NoteMessages.trashed(ids.size) { notes.restoreFromTrash(ids) })
        }
    }

    /** Trash: back where the note was (main screen or archive), with "Отменить". */
    fun restoreSelected() {
        val ids = takeSelection()
        appScope.launch {
            notes.restoreFromTrash(ids)
            messenger.send(NoteMessages.restored(ids.size) { notes.moveToTrash(ids) })
        }
    }

    /** Trash: permanent deletion. Call only after the user confirmed. */
    fun deleteSelectedForever() {
        val ids = takeSelection()
        appScope.launch {
            notes.deleteForever(ids)
            messenger.send(NoteMessages.deletedForever(ids.size))
        }
    }

    /** Trash: "Очистить". Call only after the user confirmed. */
    fun emptyTrash() {
        clearSelection()
        appScope.launch {
            val count = notes.emptyTrash()
            if (count > 0) messenger.send(NoteMessages.deletedForever(count))
        }
    }

}
