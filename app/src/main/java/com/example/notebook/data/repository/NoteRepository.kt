package com.example.notebook.data.repository

import com.example.notebook.data.database.NoteDao
import com.example.notebook.data.database.NoteEntity
import com.example.notebook.data.database.toDomain
import com.example.notebook.domain.Note
import com.example.notebook.domain.NoteColor
import com.example.notebook.domain.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * The single source of truth for notes. ViewModels call this; it talks to Room.
 * It owns the business rules about dates (when updatedAt changes) and the trash.
 *
 * @param clock current time in UTC milliseconds; tests pass a fake clock.
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun observeActiveNotes(sort: SortOrder): Flow<List<Note>> =
        noteDao.observeActive(sort.name).map { list -> list.map { it.toDomain() } }

    fun observeArchivedNotes(sort: SortOrder): Flow<List<Note>> =
        noteDao.observeArchived(sort.name).map { list -> list.map { it.toDomain() } }

    /** Main-screen notes that have a calendar day (v1.1). */
    fun observeDatedNotes(): Flow<List<Note>> =
        noteDao.observeDated().map { list -> list.map { it.toDomain() } }

    fun observeTrash(): Flow<List<Note>> =
        noteDao.observeTrash().map { list -> list.map { it.toDomain() } }

    fun observeNote(id: Long): Flow<Note?> = noteDao.observeById(id).map { it?.toDomain() }

    suspend fun getNote(id: Long): Note? = noteDao.getById(id)?.toDomain()

    suspend fun getByUuid(uuid: String): Note? = noteDao.getByUuid(uuid)?.toDomain()

    /**
     * A note written by the app itself (the work log): created if missing, otherwise its
     * title, text and tag are replaced. updatedAt changes only when something differs.
     */
    suspend fun upsertGenerated(uuid: String, title: String, content: String, categoryId: Long?) {
        val existing = noteDao.getByUuid(uuid)
        val now = clock()
        if (existing == null) {
            noteDao.insert(
                NoteEntity(uuid = uuid, title = title, content = content, categoryId = categoryId, createdAt = now, updatedAt = now),
            )
        } else if (existing.title != title || existing.content != content || existing.categoryId != categoryId) {
            noteDao.update(existing.copy(title = title, content = content, categoryId = categoryId, updatedAt = now))
        }
    }

    /**
     * Inserts a new note and returns its id. The editor calls this on the first typed
     * character, not when the FAB is pressed (spec), so empty notes never reach the database.
     */
    suspend fun createNote(
        title: String,
        content: String,
        color: NoteColor = NoteColor.DEFAULT,
        categoryId: Long? = null,
        noteDate: Long? = null,
    ): Long {
        val now = clock()
        return noteDao.insert(
            NoteEntity(
                uuid = UUID.randomUUID().toString(),
                title = title,
                content = content,
                color = color,
                categoryId = categoryId,
                noteDate = noteDate,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /**
     * Saves title and text. updatedAt changes only if something actually changed,
     * so autosave firing with the same text does not bump the date.
     * Returns false if the note no longer exists.
     */
    suspend fun updateText(id: Long, title: String, content: String): Boolean {
        val current = noteDao.getById(id) ?: return false
        if (current.title == title && current.content == content) return true
        noteDao.update(current.copy(title = title, content = content, updatedAt = clock()))
        return true
    }

    suspend fun setColor(ids: List<Long>, color: NoteColor) = noteDao.setColor(ids, color.name, clock())

    suspend fun setCategory(ids: List<Long>, categoryId: Long?) = noteDao.setCategory(ids, categoryId, clock())

    /** Puts notes on a calendar day (days since 1970-01-01), or takes them off it (null). */
    suspend fun setDate(ids: List<Long>, epochDay: Long?) = noteDao.setDate(ids, epochDay, clock())

    /** Pinning does not change updatedAt (spec). */
    suspend fun setPinned(ids: List<Long>, pinned: Boolean) = noteDao.setPinned(ids, pinned)

    /** Archiving unpins the note and does not change updatedAt (spec). */
    suspend fun archive(ids: List<Long>) = noteDao.setArchived(ids, archived = true)

    suspend fun unarchive(ids: List<Long>) = noteDao.setArchived(ids, archived = false)

    /** "Delete" in the UI: the note goes to the trash and can be restored for 30 days. */
    suspend fun moveToTrash(ids: List<Long>) = noteDao.setDeletedAt(ids, clock())

    suspend fun restoreFromTrash(ids: List<Long>) = noteDao.setDeletedAt(ids, null)

    /** Permanent deletion. The UI must ask for confirmation first. */
    suspend fun deleteForever(ids: List<Long>) = noteDao.deleteForever(ids)

    /** Permanently deletes everything in the trash. Returns how many notes were removed. */
    suspend fun emptyTrash(): Int = noteDao.emptyTrash()

    /** Called on app start: removes notes that have been in the trash for more than 30 days. */
    suspend fun purgeExpiredTrash(): Int = noteDao.deleteTrashedBefore(clock() - TRASH_RETENTION_MILLIS)

    companion object {
        val TRASH_RETENTION_MILLIS: Long = TimeUnit.DAYS.toMillis(30)
    }
}
