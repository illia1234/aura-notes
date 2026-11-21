package com.example.notebook.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.ListLayout
import com.example.notebook.domain.SortOrder
import com.example.notebook.domain.ThemeMode
import kotlinx.coroutines.launch

/**
 * Writes settings. The screen reads current values from LocalAppSettings,
 * which MainActivity keeps in sync with DataStore, so a change shows up everywhere at once.
 */
class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    fun setThemeMode(mode: ThemeMode) = save { settings.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = save { settings.setDynamicColor(enabled) }
    fun setListLayout(layout: ListLayout) = save { settings.setListLayout(layout) }
    fun setSortOrder(order: SortOrder) = save { settings.setSortOrder(order) }
    fun setEditorTextSize(size: EditorTextSize) = save { settings.setEditorTextSize(size) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
