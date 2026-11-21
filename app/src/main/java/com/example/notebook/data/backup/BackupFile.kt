package com.example.notebook.data.backup

import kotlinx.serialization.Serializable

/**
 * JSON backup format (docs/SPEC.md, "Экспорт и импорт"):
 * `{"formatVersion": 1, "exportedAt": …, "categories": […], "notes": […]}`.
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
    val createdAt: Long,
    val updatedAt: Long,
)

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
