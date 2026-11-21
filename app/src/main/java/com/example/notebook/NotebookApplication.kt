package com.example.notebook

import android.app.Application
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Created by Android before any screen. Holds the [AppContainer] for the whole app. */
class NotebookApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Spec: notes older than 30 days in the trash are removed at app start (no WorkManager).
        container.applicationScope.launch(Dispatchers.IO) {
            runCatching { container.noteRepository.purgeExpiredTrash() }
                .onSuccess { if (it > 0) Log.i(TAG, "Removed $it expired notes from trash") }
                .onFailure { Log.e(TAG, "Trash cleanup failed", it) }
        }
    }

    private companion object {
        const val TAG = "Notebook"
    }
}
