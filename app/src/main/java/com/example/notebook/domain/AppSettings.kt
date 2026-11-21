package com.example.notebook.domain

/** Text size in the editor (spec: маленький / средний / крупный). */
enum class EditorTextSize { SMALL, MEDIUM, LARGE }

/**
 * Everything on the Settings screen, with the defaults from the spec.
 * Stored in DataStore by SettingsRepository; enum names are the stored values,
 * so do not rename entries.
 *
 * Aura is dark-only and list-only (docs/REDESIGN_AURA.md), so the former theme,
 * dynamic color and grid/list settings are gone. Old values left in the settings
 * file are simply ignored.
 */
data class AppSettings(
    val sortOrder: SortOrder = SortOrder.UPDATED_NEWEST_FIRST,
    val editorTextSize: EditorTextSize = EditorTextSize.MEDIUM,
)
