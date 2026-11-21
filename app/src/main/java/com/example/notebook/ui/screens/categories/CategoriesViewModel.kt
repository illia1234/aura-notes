package com.example.notebook.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.domain.Category
import com.example.notebook.domain.CategoryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Categories screen: the list (null while loading) and create / rename / delete. */
class CategoriesViewModel(
    private val categories: CategoryRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    val list: StateFlow<List<Category>?> = categories.observeCategories()
        .map<List<Category>, List<Category>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun create(name: String): CategoryResult = categories.createCategory(name)

    suspend fun rename(id: Long, name: String): CategoryResult = categories.renameCategory(id, name)

    /** How many notes (including archive and trash) lose the category; shown in the delete dialog. */
    suspend fun countNotes(id: Long): Int = categories.countNotesInCategory(id)

    fun delete(id: Long) {
        appScope.launch { categories.deleteCategory(id) }
    }
}
