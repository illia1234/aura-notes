package com.example.notebook.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteColor

/** Row of the `notes` table. Field list follows docs/SPEC.md, "Таблица notes". */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            // Deleting a category keeps its notes; they just lose the category.
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("categoryId"),
        // uuid identifies a note across devices and backups, so import never duplicates it.
        Index(value = ["uuid"], unique = true),
    ],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val title: String,
    val content: String,
    val color: NoteColor = NoteColor.DEFAULT,
    val categoryId: Long? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

fun NoteEntity.toDomain() = Note(
    id = id,
    uuid = uuid,
    title = title,
    content = content,
    color = color,
    categoryId = categoryId,
    isPinned = isPinned,
    isArchived = isArchived,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Note.toEntity() = NoteEntity(
    id = id,
    uuid = uuid,
    title = title,
    content = content,
    color = color,
    categoryId = categoryId,
    isPinned = isPinned,
    isArchived = isArchived,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
