package com.example.notebook.data.backup

import kotlinx.serialization.Serializable

/**
 * JSON backup format (docs/SPEC.md, "Экспорт и импорт"):
 * `{"formatVersion": 2, "exportedAt": …, "categories": […], "notes": […], "dayOverrides": […]}`.
 * Format 3 (v1.2) adds "workShifts" and "schedule". Files of formats 1 and 2 still import.
 *
 * A note refers to its category by *name*, not by database id, because ids differ
 * between devices. Dates are UTC milliseconds. Fields with defaults may be missing
 * in a file, and unknown fields are ignored, so small format changes stay compatible.
 */
@Serializable
data class BackupFile(
    val formatVersion: Int,
    val exportedAt: Long,
    val categories: List<BackupCategory> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
    /** Manual shift-schedule exceptions (format 2, v1.1; no longer used by the app). */
    val dayOverrides: List<BackupDayOverride> = emptyList(),
    /** Worked days with hours (format 3, v1.2). */
    val workShifts: List<BackupWorkShift> = emptyList(),
    /** Shift schedule used to predict future shifts (format 3), or null. */
    val schedule: BackupSchedule? = null,
)

@Serializable
data class BackupCategory(
    val name: String,
    val createdAt: Long = 0,
)

@Serializable
data class BackupNote(
    val uuid: String,
    val title: String = "",
    val content: String = "",
    /** NoteColor name, e.g. "SAGE". Unknown names become DEFAULT on import. */
    val color: String = "DEFAULT",
    /** Category name, or null for none. */
    val category: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    /** Not null = the note is in the trash (moved there at this time). */
    val deletedAt: Long? = null,
    /** Calendar day (days since 1970-01-01), or null. Format 2 (v1.1); missing in format 1 files. */
    val noteDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** One schedule exception: that day is a shift (true) or a day off (false). */
@Serializable
data class BackupDayOverride(val epochDay: Long, val isWorkDay: Boolean)

/** A worked day: day (days since 1970-01-01) and shift time in minutes after midnight. */
@Serializable
data class BackupWorkShift(val epochDay: Long, val startMinute: Int, val endMinute: Int)

/** Shift schedule; pattern is a SchedulePattern name. */
@Serializable
data class BackupSchedule(val pattern: String, val workDays: Int, val offDays: Int, val startEpochDay: Long)

/** Summary shown after import: "Добавлено 12, обновлено 3, пропущено 40". */
data class ImportResult(val added: Int, val updated: Int, val skipped: Int)

/** Why a file could not be imported. Nothing in the database is changed in these cases. */
class BackupException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        /** Not JSON, or JSON that is not a Notebook backup. */
        NOT_A_BACKUP,

        /** Made by a newer app version with a format this version does not know. */
        NEWER_VERSION,

        /** The file could not be opened, read or written. */
        IO_ERROR,
    }
}
