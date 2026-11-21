package com.example.notebook.domain

/** Grid (2 columns, staggered) or list (1 column) on the main, archive and trash screens. */
enum class ListLayout { GRID, LIST }

/** Text size in the editor (spec: маленький / средний / крупный). */
enum class EditorTextSize { SMALL, MEDIUM, LARGE }

/**
 * Everything on the Settings screen, with the defaults from docs/SPEC.md.
 * Stored in DataStore by SettingsRepository; enum names are the stored values,
 * so do not rename entries.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val listLayout: ListLayout = ListLayout.GRID,
    val sortOrder: SortOrder = SortOrder.UPDATED_NEWEST_FIRST,
    val editorTextSize: EditorTextSize = EditorTextSize.MEDIUM,
)
