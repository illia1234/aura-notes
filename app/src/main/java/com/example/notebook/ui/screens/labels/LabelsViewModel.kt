package com.example.notebook.ui.screens.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.domain.Category
import com.example.notebook.domain.CategoryResult
import com.example.notebook.domain.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LabelsUiState(
    val labels: List<Category> = emptyList(),
    val archiveCount: Int = 0,
    val trashCount: Int = 0,
    val isLoading: Boolean = true,
)

/**
 * "Метки" tab: tags with note counts (create / rename / delete) and the entries
 * to Archive and Trash. In the database a tag is still a "category" (one per note).
 */
class LabelsViewModel(
    private val categories: CategoryRepository,
    notes: NoteRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    val state: StateFlow<LabelsUiState> = combine(
        categories.observeCategories(),
        notes.observeArchivedNotes(SortOrder.UPDATED_NEWEST_FIRST),
        notes.observeTrash(),
    ) { labels, archived, trash ->
        LabelsUiState(labels, archived.size, trash.size, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LabelsUiState())

    suspend fun create(name: String): CategoryResult = categories.createCategory(name)

    suspend fun rename(id: Long, name: String): CategoryResult = categories.renameCategory(id, name)

    /** How many notes (including archive and trash) lose the tag; shown in the delete dialog. */
    suspend fun countNotes(id: Long): Int = categories.countNotesInCategory(id)

    fun delete(id: Long) {
        appScope.launch { categories.deleteCategory(id) }
    }
}
