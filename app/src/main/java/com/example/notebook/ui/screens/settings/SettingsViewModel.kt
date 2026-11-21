package com.example.notebook.ui.screens.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.R
import com.example.notebook.data.backup.BackupException
import com.example.notebook.data.backup.BackupRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.ListLayout
import com.example.notebook.domain.SortOrder
import com.example.notebook.domain.ThemeMode
import com.example.notebook.ui.UserMessage
import com.example.notebook.ui.UserMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes settings and runs export / import. The screen reads current values from
 * LocalAppSettings, which MainActivity keeps in sync with DataStore.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val backup: BackupRepository,
    private val messenger: UserMessenger,
    private val appScope: CoroutineScope,
) : ViewModel() {

    fun setThemeMode(mode: ThemeMode) = save { settings.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = save { settings.setDynamicColor(enabled) }
    fun setListLayout(layout: ListLayout) = save { settings.setListLayout(layout) }
    fun setSortOrder(order: SortOrder) = save { settings.setSortOrder(order) }
    fun setEditorTextSize(size: EditorTextSize) = save { settings.setEditorTextSize(size) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /** True while an export or import is running; the buttons are disabled meanwhile. */
    var isBusy by mutableStateOf(false)
        private set

    val messages: Flow<UserMessage> = messenger.messages

    /** Default file name in the save dialog, e.g. notebook-backup-2026-11-21.json (spec). */
    fun suggestedFileName(): String =
        "notebook-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) + ".json"

    fun export(uri: Uri) = runBackup {
        val count = backup.exportTo(uri)
        UserMessage(text = { it.getString(R.string.backup_export_done, count) })
    }

    fun import(uri: Uri) = runBackup {
        val r = backup.importFrom(uri)
        UserMessage(text = { it.getString(R.string.backup_import_done, r.added, r.updated, r.skipped) })
    }

    /** Runs in the app scope so leaving the screen does not cut an import in half. */
    private fun runBackup(work: suspend () -> UserMessage) {
        if (isBusy) return
        isBusy = true
        appScope.launch {
            val message = try {
                work()
            } catch (e: BackupException) {
                val res = when (e.reason) {
                    BackupException.Reason.NOT_A_BACKUP -> R.string.backup_error_not_backup
                    BackupException.Reason.NEWER_VERSION -> R.string.backup_error_newer
                    BackupException.Reason.IO_ERROR -> R.string.backup_error_io
                }
                UserMessage(text = { it.getString(res) })
            }
            messenger.send(message)
            isBusy = false
        }
    }
}
