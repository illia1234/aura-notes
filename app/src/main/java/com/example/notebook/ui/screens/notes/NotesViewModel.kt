package com.example.notebook.ui.screens.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteColor
import com.example.notebook.domain.NoteSearch
import com.example.notebook.domain.SortOrder
import com.example.notebook.ui.NoteMessages
import com.example.notebook.ui.UserMessage
import com.example.notebook.ui.UserMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
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

/** Search results for the (debounced) query that produced them. */
data class SearchState(val query: String = "", val results: List<NoteItem> = emptyList()) {
    val showNothingFound: Boolean get() = query.isNotBlank() && results.isEmpty()
}

/**
 * Main screen: the list of notes, search, sort order and the multi-select mode.
 * Selection and the query live here (not in the composable), so they survive rotation.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NotesViewModel(
    private val notes: NoteRepository,
    categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val messenger: UserMessenger,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    /** The saved sort order (DataStore); changing it re-queries the database. */
    val sortOrder: StateFlow<SortOrder> = settings.sortOrder
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortOrder.UPDATED_NEWEST_FIRST)

    /** Main-screen notes in the chosen order. Shared by the list and by search. */
    private val activeNotes: Flow<List<Note>> = settings.sortOrder
        .flatMapLatest { order -> notes.observeActiveNotes(order) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val categoryNames: Flow<Map<Long, String>> = categories.observeCategories()
        .map { list -> list.associate { it.id to it.name } }

    /** Search text. Compose state (like the editor fields) so typing stays smooth. */
    var query by mutableStateOf("")
        private set

    /** Results update ~300 ms after typing stops (spec); clearing the field is instant. */
    val search: StateFlow<SearchState> = combine(
        snapshotFlow { query }.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS },
        activeNotes,
        categoryNames,
    ) { q, noteList, names ->
        SearchState(q, NoteSearch.search(noteList, q).map { NoteItem(it, it.categoryId?.let(names::get)) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun onQueryChange(value: String) {
        query = value
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settings.setSortOrder(order) }
    }

    val uiState: StateFlow<NotesUiState> = combine(
        activeNotes,
        categoryNames,
        selection,
    ) { noteList, names, selected ->
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

private const val SEARCH_DEBOUNCE_MS = 300L
