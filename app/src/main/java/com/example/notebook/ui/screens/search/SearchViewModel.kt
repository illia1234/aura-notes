package com.example.notebook.ui.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.NoteSearch
import com.example.notebook.ui.screens.notes.NoteItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Search results for the (debounced) query that produced them. */
data class SearchState(val query: String = "", val results: List<NoteItem> = emptyList()) {
    val showNothingFound: Boolean get() = query.isNotBlank() && results.isEmpty()
}

/**
 * The "Поиск" tab. Rules are unchanged (docs/SPEC.md): case-insensitive substring in
 * title and text, title matches first, archive and trash excluded, ~300 ms debounce.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    notes: NoteRepository,
    categories: CategoryRepository,
    settings: SettingsRepository,
) : ViewModel() {

    /** Compose state (like the editor fields) so typing stays smooth. */
    var query by mutableStateOf("")
        private set

    fun onQueryChange(value: String) {
        query = value
    }

    val state: StateFlow<SearchState> = combine(
        snapshotFlow { query }.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS },
        settings.sortOrder.flatMapLatest { notes.observeActiveNotes(it) },
        categories.observeCategories(),
    ) { q, noteList, cats ->
        val names = cats.associate { it.id to it.name }
        SearchState(q, NoteSearch.search(noteList, q).map { NoteItem(it, it.categoryId?.let(names::get)) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
