package com.example.notebook

import android.content.Context
import com.example.notebook.data.backup.BackupRepository
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.notebook.ui.UserMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency injection: one place that creates the long-lived objects
 * (database, repositories) once and hands them out. ViewModels get what they need
 * from here. This replaces a framework like Hilt, which a small app does not need.
 */
class AppContainer(context: Context) {

    /**
     * Work that must finish even if the screen that started it closes (the final
     * autosave when leaving the editor, undo actions). Runs on the main thread;
     * Room moves the actual database work to its own background threads.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: NotebookDatabase by lazy { NotebookDatabase.create(context) }

    val noteRepository: NoteRepository by lazy { NoteRepository(database.noteDao()) }

    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database) }

    val userMessenger = UserMessenger()

    val backupRepository: BackupRepository by lazy { BackupRepository(database, context.contentResolver) }

    /** One DataStore per file for the whole app (DataStore requires a single instance). */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") }),
        )
    }
}
