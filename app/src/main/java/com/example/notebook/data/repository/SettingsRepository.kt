package com.example.notebook.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.notebook.domain.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * User settings stored with DataStore Preferences (a small key-value file).
 * Values are stored as enum names; an unknown or missing value falls back to the default.
 * Stage 6 adds theme, dynamic color, list layout and text size here.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /** If the file cannot be read, use defaults instead of crashing. */
    private val preferences: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val sortOrder: Flow<SortOrder> = preferences
        .map { prefs -> prefs[SORT_ORDER].toEnum(SortOrder.UPDATED_NEWEST_FIRST) }
        .distinctUntilChanged()

    suspend fun setSortOrder(order: SortOrder) {
        dataStore.edit { it[SORT_ORDER] = order.name }
    }

    private companion object {
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
