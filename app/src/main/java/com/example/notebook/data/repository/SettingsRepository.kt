package com.example.notebook.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.notebook.domain.AppSettings
import com.example.notebook.domain.AuraStyle
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.SchedulePattern
import com.example.notebook.domain.ShiftSchedule
import com.example.notebook.domain.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * User settings stored with DataStore Preferences (a small key-value file).
 * Values are stored as enum names; an unknown or missing value falls back to the default,
 * so a damaged or newer file never crashes the app.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /** If the file cannot be read, use defaults instead of crashing. */
    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<AppSettings> = preferences.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            sortOrder = prefs[SORT_ORDER].toEnum(defaults.sortOrder),
            editorTextSize = prefs[EDITOR_TEXT_SIZE].toEnum(defaults.editorTextSize),
            style = prefs[STYLE].toEnum(defaults.style),
            schedule = prefs.schedule(),
        )
    }.distinctUntilChanged()

    val sortOrder: Flow<SortOrder> = settings.map { it.sortOrder }.distinctUntilChanged()

    val schedule: Flow<ShiftSchedule?> = settings.map { it.schedule }.distinctUntilChanged()

    suspend fun setSortOrder(order: SortOrder) = dataStore.edit { it[SORT_ORDER] = order.name }

    suspend fun setEditorTextSize(size: EditorTextSize) = dataStore.edit { it[EDITOR_TEXT_SIZE] = size.name }

    suspend fun setStyle(style: AuraStyle) = dataStore.edit { it[STYLE] = style.name }

    /** Saves the shift schedule, or removes it (null). */
    suspend fun setSchedule(schedule: ShiftSchedule?) = dataStore.edit { prefs ->
        if (schedule == null) {
            prefs.remove(SCHEDULE_PATTERN)
            prefs.remove(SCHEDULE_WORK)
            prefs.remove(SCHEDULE_OFF)
            prefs.remove(SCHEDULE_START)
        } else {
            prefs[SCHEDULE_PATTERN] = schedule.pattern.name
            prefs[SCHEDULE_WORK] = schedule.workDays
            prefs[SCHEDULE_OFF] = schedule.offDays
            prefs[SCHEDULE_START] = schedule.startEpochDay
        }
    }

    /** A schedule is read only if every part is present and valid; otherwise "not set up". */
    private fun Preferences.schedule(): ShiftSchedule? {
        val pattern = SchedulePattern.entries.firstOrNull { it.name == this[SCHEDULE_PATTERN] } ?: return null
        return runCatching {
            ShiftSchedule(
                pattern = pattern,
                workDays = this[SCHEDULE_WORK] ?: pattern.defaultWork,
                offDays = this[SCHEDULE_OFF] ?: pattern.defaultOff,
                startEpochDay = this[SCHEDULE_START] ?: 0L,
            )
        }.getOrNull()
    }

    private companion object {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val EDITOR_TEXT_SIZE = stringPreferencesKey("editor_text_size")
        val STYLE = stringPreferencesKey("style")
        val SCHEDULE_PATTERN = stringPreferencesKey("schedule_pattern")
        val SCHEDULE_WORK = intPreferencesKey("schedule_work_days")
        val SCHEDULE_OFF = intPreferencesKey("schedule_off_days")
        val SCHEDULE_START = longPreferencesKey("schedule_start_epoch_day")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
