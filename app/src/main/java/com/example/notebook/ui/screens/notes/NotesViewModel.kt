package com.example.notebook.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteColor
import com.example.notebook.domain.SortOrder
import com.example.notebook.ui.NoteMessages
import com.example.notebook.ui.UserMessage
import com.example.notebook.ui.UserMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A note card's data: the note plus its category name, if any. */
data class NoteItem(val note: Note, val categoryName: String?)

data class NotesUiState(
    val pinned: List<NoteItem> = emptyList(),
    val others: List<NoteItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && pinned.isEmpty() && others.isEmpty()
    val isSelecting: Boolean get() = selectedIds.isNotEmpty()

    /** Section titles are shown only when at least one note is pinned (spec). */
    val showSections: Boolean get() = pinned.isNotEmpty()

    val selectedNotes: List<Note>
        get() = (pinned + others).map { it.note }.filter { it.id in selectedIds }
}

/**
 * Main screen: the list of notes and the multi-select mode.
 * Selection lives here (not in the composable), so it survives screen rotation.
 */
class NotesViewModel(
    private val notes: NoteRepository,
    categories: CategoryRepository,
    private val messenger: UserMessenger,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<NotesUiState> = combine(
        // Sort order becomes a setting in stage 4.
        notes.observeActiveNotes(SortOrder.UPDATED_NEWEST_FIRST),
        categories.observeCategories(),
        selection,
    ) { noteList, categoryList, selected ->
        val names = categoryList.associate { it.id to it.name }
        val items = noteList.map { NoteItem(it, it.categoryId?.let(names::get)) }
        val visibleIds = noteList.mapTo(HashSet()) { it.id }
        NotesUiState(
            pinned = items.filter { it.note.isPinned },
            others = items.filterNot { it.note.isPinned },
            // Notes that disappeared (e.g. deleted elsewhere) drop out of the selection.
            selectedIds = selected intersect visibleIds,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    /** Snackbar messages from this screen and from the editor. */
    val messages: Flow<UserMessage> = messenger.messages

    fun toggleSelection(id: Long) = selection.update { if (id in it) it - id else it + id }

    fun clearSelection() = selection.update { emptySet() }

    /** Pins all selected notes, or unpins them if all are already pinned. */
    fun togglePinSelected() {
        val selected = uiState.value.selectedNotes
        val pin = !selected.all { it.isPinned }
        val ids = selected.map { it.id }
        clearSelection()
        appScope.launch { notes.setPinned(ids, pin) }
    }

    fun setColorForSelected(color: NoteColor) {
        val ids = uiState.value.selectedIds.toList()
        clearSelection()
        appScope.launch { notes.setColor(ids, color) }
    }

    fun archiveSelected() {
        val selected = uiState.value.selectedNotes
        val ids = selected.map { it.id }
        // Archiving unpins; remember which were pinned so "Отменить" restores them exactly.
        val pinnedIds = selected.filter { it.isPinned }.map { it.id }
        clearSelection()
        appScope.launch {
            notes.archive(ids)
            messenger.send(
                NoteMessages.archived(ids.size) {
                    notes.unarchive(ids)
                    if (pinnedIds.isNotEmpty()) notes.setPinned(pinnedIds, true)
                },
            )
        }
    }

    fun trashSelected() {
        val ids = uiState.value.selectedIds.toList()
        clearSelection()
        appScope.launch {
            notes.moveToTrash(ids)
            messenger.send(NoteMessages.trashed(ids.size) { notes.restoreFromTrash(ids) })
        }
    }

    /** Runs a snackbar's action (e.g. undo) in the app scope, so it finishes even if the screen changes. */
    fun runMessageAction(message: UserMessage) {
        val action = message.action ?: return
        appScope.launch { action() }
    }
}
