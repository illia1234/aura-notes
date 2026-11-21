package com.example.notebook.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.AppSettings
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Settings are stored in a DataStore file; here it is a temporary file on the computer. */
class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun file(name: String) = folder.newFile("$name.preferences_pb").also { it.delete() }

    @Test
    fun defaultsAreRememberedAfterChange() = runTest {
        val f = file("settings")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { f })
        val settings = SettingsRepository(store)

        assertEquals(AppSettings(SortOrder.UPDATED_NEWEST_FIRST, EditorTextSize.MEDIUM), settings.settings.first())

        settings.setSortOrder(SortOrder.CREATED_OLDEST_FIRST)
        settings.setEditorTextSize(EditorTextSize.LARGE)

        // A new repository on the same file sees the saved values (like after an app restart).
        assertEquals(
            AppSettings(SortOrder.CREATED_OLDEST_FIRST, EditorTextSize.LARGE),
            SettingsRepository(store).settings.first(),
        )
    }

    @Test
    fun unknownStoredValueFallsBackToDefault() = runTest {
        val f = file("settings2")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { f })
        store.edit { it[stringPreferencesKey("sort_order")] = "SOMETHING_NEW" }
        assertEquals(SortOrder.UPDATED_NEWEST_FIRST, SettingsRepository(store).sortOrder.first())
    }

    @Test
    fun oldThemeAndLayoutKeysFromPreviousVersionAreIgnored() = runTest {
        // Before the Aura redesign the file also held theme and grid/list settings.
        val f = file("settings3")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { f })
        store.edit {
            it[stringPreferencesKey("theme_mode")] = "LIGHT"
            it[stringPreferencesKey("list_layout")] = "GRID"
            it[stringPreferencesKey("editor_text_size")] = "SMALL"
        }
        assertEquals(EditorTextSize.SMALL, SettingsRepository(store).settings.first().editorTextSize)
    }
}
