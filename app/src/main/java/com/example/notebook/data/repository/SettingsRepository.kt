package com.example.notebook.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.notebook.domain.AppSettings
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.ListLayout
import com.example.notebook.domain.SortOrder
import com.example.notebook.domain.ThemeMode
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
            themeMode = prefs[THEME].toEnum(defaults.themeMode),
            dynamicColor = prefs[DYNAMIC_COLOR] ?: defaults.dynamicColor,
            listLayout = prefs[LIST_LAYOUT].toEnum(defaults.listLayout),
            sortOrder = prefs[SORT_ORDER].toEnum(defaults.sortOrder),
            editorTextSize = prefs[EDITOR_TEXT_SIZE].toEnum(defaults.editorTextSize),
        )
    }.distinctUntilChanged()

    val sortOrder: Flow<SortOrder> = settings.map { it.sortOrder }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[THEME] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[DYNAMIC_COLOR] = enabled }

    suspend fun setListLayout(layout: ListLayout) = dataStore.edit { it[LIST_LAYOUT] = layout.name }

    suspend fun setSortOrder(order: SortOrder) = dataStore.edit { it[SORT_ORDER] = order.name }

    suspend fun setEditorTextSize(size: EditorTextSize) = dataStore.edit { it[EDITOR_TEXT_SIZE] = size.name }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LIST_LAYOUT = stringPreferencesKey("list_layout")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val EDITOR_TEXT_SIZE = stringPreferencesKey("editor_text_size")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
