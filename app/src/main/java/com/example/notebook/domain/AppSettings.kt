package com.example.notebook.domain

/** Text size in the editor (spec: маленький / средний / крупный). */
enum class EditorTextSize { SMALL, MEDIUM, LARGE }

/** Background look of the app (v1.1, "минималистичный футуризм"). */
enum class AuraStyle { BLACK, AURORA, NEON, SUNSET, GRAPHITE }

/**
 * Everything on the Settings screen, with the defaults from the spec.
 * Stored in DataStore by SettingsRepository; enum names are the stored values,
 * so do not rename entries. Unknown or old values are ignored.
 *
 * The app language is not here: Android stores the per-app language itself
 * (see LocaleSettings).
 */
data class AppSettings(
    val sortOrder: SortOrder = SortOrder.UPDATED_NEWEST_FIRST,
    val editorTextSize: EditorTextSize = EditorTextSize.MEDIUM,
    val style: AuraStyle = AuraStyle.BLACK,
    /** Shift schedule for the calendar; null = not set up. */
    val schedule: ShiftSchedule? = null,
)
