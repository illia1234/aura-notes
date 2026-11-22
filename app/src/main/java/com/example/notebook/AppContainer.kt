package com.example.notebook

import android.content.Context
import com.example.notebook.data.backup.BackupRepository
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.WorkRepository
import com.example.notebook.ui.util.workLogLabels
import com.example.notebook.data.repository.SettingsRepository
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.notebook.ui.UserMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Manual dependency injection: one place that creates the long-lived objects
 * (database, repositories) once and hands them out. ViewModels get what they need
 * from here. This replaces a framework like Hilt, which a small app does not need.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /**
     * Work that must finish even if the screen that started it closes (the final
     * autosave when leaving the editor, undo actions). Runs on the main thread;
     * Room moves the actual database work to its own background threads.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: NotebookDatabase by lazy { NotebookDatabase.create(context) }

    val noteRepository: NoteRepository by lazy { NoteRepository(database.noteDao()) }

    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database) }

    /** Worked days and the "Mondieu" note (v1.2). The note is written in the current app language. */
    val workRepository: WorkRepository by lazy {
        WorkRepository(database.workShiftDao(), noteRepository, categoryRepository, settingsRepository) {
            workLogLabels(appContext)
        }
    }

    val userMessenger = UserMessenger()

    /** Tag filter of the home tab. Shared so the "Метки" tab can open home already filtered. */
    val homeCategoryFilter = MutableStateFlow<Long?>(null)

    val backupRepository: BackupRepository by lazy { BackupRepository(database, context.contentResolver, settingsRepository) }

    /** One DataStore per file for the whole app (DataStore requires a single instance). */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") }),
        )
    }
}
