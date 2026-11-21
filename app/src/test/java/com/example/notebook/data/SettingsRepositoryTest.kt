package com.example.notebook.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.AppSettings
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.ListLayout
import com.example.notebook.domain.SortOrder
import com.example.notebook.domain.ThemeMode
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

    @Test
    fun sortOrderDefaultsToNewestEditedFirstAndIsRemembered() = runTest {
        val file = folder.newFile("settings.preferences_pb").also { it.delete() }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        val settings = SettingsRepository(store)

        assertEquals(SortOrder.UPDATED_NEWEST_FIRST, settings.sortOrder.first())
        settings.setSortOrder(SortOrder.CREATED_OLDEST_FIRST)
        assertEquals(SortOrder.CREATED_OLDEST_FIRST, settings.sortOrder.first())
    }

    @Test
    fun allSettingsHaveSpecDefaultsAndAreRemembered() = runTest {
        val file = folder.newFile("settings3.preferences_pb").also { it.delete() }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        val settings = SettingsRepository(store)

        assertEquals(AppSettings(), settings.settings.first())
        assertEquals(ThemeMode.SYSTEM, AppSettings().themeMode)
        assertEquals(ListLayout.GRID, AppSettings().listLayout)
        assertEquals(EditorTextSize.MEDIUM, AppSettings().editorTextSize)

        settings.setThemeMode(ThemeMode.DARK)
        settings.setDynamicColor(false)
        settings.setListLayout(ListLayout.LIST)
        settings.setEditorTextSize(EditorTextSize.LARGE)

        // A new repository on the same file sees the saved values (like after an app restart).
        val reopened = SettingsRepository(store).settings.first()
        assertEquals(
            AppSettings(
                themeMode = ThemeMode.DARK,
                dynamicColor = false,
                listLayout = ListLayout.LIST,
                editorTextSize = EditorTextSize.LARGE,
            ),
            reopened,
        )
    }

    @Test
    fun unknownStoredValueFallsBackToDefault() = runTest {
        val file = folder.newFile("settings2.preferences_pb").also { it.delete() }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        store.edit { it[stringPreferencesKey("sort_order")] = "SOMETHING_NEW" }
        assertEquals(SortOrder.UPDATED_NEWEST_FIRST, SettingsRepository(store).sortOrder.first())
    }
}
