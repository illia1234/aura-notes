package com.example.notebook.domain

/**
 * A note as the rest of the app sees it. Screens and ViewModels use this class,
 * never the Room entity, so database details stay inside the data layer.
 * All dates are UTC milliseconds; they are converted to local time only for display.
 */
data class Note(
    val id: Long,
    val uuid: String,
    val title: String,
    val content: String,
    val color: NoteColor,
    val categoryId: Long?,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isInTrash: Boolean get() = deletedAt != null

    /** Title and text are empty or only whitespace (spec: such a new note is not kept). */
    val isBlank: Boolean get() = title.isBlank() && content.isBlank()
}
