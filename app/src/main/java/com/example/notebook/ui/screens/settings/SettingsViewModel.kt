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
import com.example.notebook.data.repository.WorkRepository
import com.example.notebook.domain.AuraStyle
import com.example.notebook.domain.EditorTextSize
import com.example.notebook.domain.SortOrder
import com.example.notebook.ui.UserMessage
import com.example.notebook.ui.UserMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes settings and runs export / import (results appear in the app-wide snackbar). The screen reads current values from
 * LocalAppSettings, which MainActivity keeps in sync with DataStore.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val backup: BackupRepository,
    private val messenger: UserMessenger,
    private val appScope: CoroutineScope,
    private val work: WorkRepository,
) : ViewModel() {

    fun setSortOrder(order: SortOrder) = save { settings.setSortOrder(order) }
    fun setEditorTextSize(size: EditorTextSize) = save { settings.setEditorTextSize(size) }
    fun setStyle(style: AuraStyle) = save { settings.setStyle(style) }

    /** Rate typed by the user, e.g. "7" or "7,50". Returns false if it is not a valid amount. */
    fun setHourlyRate(text: String): Boolean {
        val euros = text.trim().replace(',', '.').toBigDecimalOrNull() ?: return false
        if (euros.signum() < 0 || euros > java.math.BigDecimal(1000)) return false
        val cents = euros.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toInt()
        appScope.launch {
            settings.setHourlyRateCents(cents)
            work.refreshNote() // the note shows money, so rewrite it with the new rate
        }
        return true
    }

    fun setDefaultShift(startMinute: Int, endMinute: Int) = save { settings.setDefaultShift(startMinute, endMinute) }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /** True while an export or import is running; the buttons are disabled meanwhile. */
    var isBusy by mutableStateOf(false)
        private set


    /** Default file name in the save dialog, e.g. notebook-backup-2026-11-21.json (spec). */
    fun suggestedFileName(): String =
        "notebook-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) + ".json"

    fun export(uri: Uri) = runBackup {
        val count = backup.exportTo(uri)
        UserMessage(text = { it.getString(R.string.backup_export_done, count) })
    }

    fun import(uri: Uri) = runBackup {
        val r = backup.importFrom(uri)
        work.refreshNote() // imported shifts appear in the "Mondieu" note
        UserMessage(text = { it.getString(R.string.backup_import_done, r.added, r.updated, r.skipped) })
    }

    /** Runs in the app scope so leaving the screen does not cut an import in half. */
    private fun runBackup(job: suspend () -> UserMessage) {
        if (isBusy) return
        isBusy = true
        appScope.launch {
            val message = try {
                job()
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
