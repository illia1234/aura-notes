package com.example.notebook.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.Category
import com.example.notebook.domain.CategoryResult
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteColor
import com.example.notebook.domain.SortOrder
import com.example.notebook.ui.NoteMessages
import com.example.notebook.ui.UserMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A note card's data: the note plus its tag (category) name, if any. */
data class NoteItem(val note: Note, val categoryName: String?)

data class NotesUiState(
    val notes: List<NoteItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    /** All tags with counts, for the chips. */
    val categories: List<Category> = emptyList(),
    /** Tag filter; null = "Все". */
    val filterCategoryId: Long? = null,
) {
    val isEmpty: Boolean get() = !isLoading && notes.isEmpty()
    val isSelecting: Boolean get() = selectedIds.isNotEmpty()

    val selectedNotes: List<Note>
        get() = notes.map { it.note }.filter { it.id in selectedIds }
}

/**
 * The note list of the "Главная" tab:
 * notes, tag filter, sort order and multi-select. Selection lives here (not in the
 * composable), so it survives rotation.
 *
 * @param homeFilter tag filter shared with the "Метки" tab, which can open the home
 *   screen already filtered by a tag.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val notes: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val settings: SettingsRepository,
    private val homeFilter: MutableStateFlow<Long?>,
    private val messenger: UserMessenger,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    /** The saved sort order (DataStore); changing it re-queries the database. */
    val sortOrder: StateFlow<SortOrder> = settings.sortOrder
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortOrder.UPDATED_NEWEST_FIRST)

    private val activeNotes: Flow<List<Note>> = settings.sortOrder
        .flatMapLatest { order -> notes.observeActiveNotes(order) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val filter: MutableStateFlow<Long?> = homeFilter

    fun setFilter(categoryId: Long?) {
        filter.value = categoryId
        clearSelection()
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settings.setSortOrder(order) }
    }

    val uiState: StateFlow<NotesUiState> = combine(
        activeNotes,
        categoryRepository.observeCategories(),
        filter,
        selection,
    ) { noteList, cats, filterId, selected ->
        val names = cats.associate { it.id to it.name }
        // If the filtered tag was deleted, fall back to "Все".
        val activeFilter = filterId?.takeIf { it in names }
        val shown = noteList.filter { activeFilter == null || it.categoryId == activeFilter }
        val items = shown.map { NoteItem(it, it.categoryId?.let(names::get)) }
        NotesUiState(
            notes = items,
            // Notes that disappeared (deleted, filtered out) drop out of the selection.
            selectedIds = selected intersect shown.mapTo(HashSet()) { it.id },
            isLoading = false,
            categories = cats,
            filterCategoryId = activeFilter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun toggleSelection(id: Long) = selection.update { if (id in it) it - id else it + id }

    fun clearSelection() = selection.update { emptySet() }

    fun setColorForSelected(color: NoteColor) {
        val ids = uiState.value.selectedIds.toList()
        clearSelection()
        appScope.launch { notes.setColor(ids, color) }
    }

    fun setCategoryForSelected(categoryId: Long?) {
        val ids = uiState.value.selectedIds.toList()
        clearSelection()
        appScope.launch { notes.setCategory(ids, categoryId) }
    }

    /** "+ Новая метка" from selection mode: create it, then assign it to the selected notes. */
    suspend fun createCategoryForSelected(name: String): CategoryResult {
        val result = categoryRepository.createCategory(name)
        if (result is CategoryResult.Success) setCategoryForSelected(result.id)
        return result
    }

    fun archiveSelected() {
        val ids = uiState.value.selectedIds.toList()
        clearSelection()
        appScope.launch {
            notes.archive(ids)
            messenger.send(NoteMessages.archived(ids.size) { notes.unarchive(ids) })
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
}
