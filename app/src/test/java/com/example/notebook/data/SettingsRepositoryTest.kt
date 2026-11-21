package com.example.notebook.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.notebook.data.repository.SettingsRepository
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
    fun unknownStoredValueFallsBackToDefault() = runTest {
        val file = folder.newFile("settings2.preferences_pb").also { it.delete() }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        store.edit { it[stringPreferencesKey("sort_order")] = "SOMETHING_NEW" }
        assertEquals(SortOrder.UPDATED_NEWEST_FIRST, SettingsRepository(store).sortOrder.first())
    }
}
