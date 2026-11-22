package com.example.notebook.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * All SQL for notes. Functions returning Flow re-emit automatically whenever the
 * table changes, which is what keeps the UI in sync with the database.
 * `suspend` functions run off the main thread (Room handles that).
 *
 * [sort] parameters take a SortOrder name; see ORDER_BY below.
 */
@Dao
interface NoteDao {

    /** Main screen: not archived, not in trash. (Since v1.1 favorites/pins no longer affect the order.) */
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND deletedAt IS NULL ORDER BY $ORDER_BY")
    fun observeActive(sort: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND deletedAt IS NULL ORDER BY $ORDER_BY")
    fun observeArchived(sort: String): Flow<List<NoteEntity>>

    /** Trash: most recently deleted first. */
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC, id DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    /** Main-screen notes that have a calendar day (for the calendar tab). */
    @Query("SELECT * FROM notes WHERE noteDate IS NOT NULL AND isArchived = 0 AND deletedAt IS NULL ORDER BY noteDate, updatedAt DESC")
    fun observeDated(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): NoteEntity?

    /** Every note, including archive and trash. Used by export. */
    @Query("SELECT * FROM notes ORDER BY id")
    suspend fun getAll(): List<NoteEntity>

    /** Fails (instead of overwriting) if the uuid already exists. */
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    // --- Changes that do NOT touch updatedAt (spec: pinning and archiving keep the date) ---

    @Query("UPDATE notes SET isPinned = :pinned WHERE id IN (:ids)")
    suspend fun setPinned(ids: List<Long>, pinned: Boolean)

    /** Archiving also unpins (spec). Unarchiving leaves isPinned as it is (false). */
    @Query("UPDATE notes SET isArchived = :archived, isPinned = CASE WHEN :archived THEN 0 ELSE isPinned END WHERE id IN (:ids)")
    suspend fun setArchived(ids: List<Long>, archived: Boolean)

    @Query("UPDATE notes SET deletedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun setDeletedAt(ids: List<Long>, deletedAt: Long?)

    // --- Changes that count as edits: updatedAt changes only for notes that really change ---

    @Query("UPDATE notes SET color = :color, updatedAt = :now WHERE id IN (:ids) AND color != :color")
    suspend fun setColor(ids: List<Long>, color: String, now: Long)

    /** `IS NOT` (not `!=`) so that comparing with NULL works. */
    @Query("UPDATE notes SET categoryId = :categoryId, updatedAt = :now WHERE id IN (:ids) AND categoryId IS NOT :categoryId")
    suspend fun setCategory(ids: List<Long>, categoryId: Long?, now: Long)

    /** Calendar day of a note (v1.1). Changing it is an edit, so updatedAt changes too. */
    @Query("UPDATE notes SET noteDate = :epochDay, updatedAt = :now WHERE id IN (:ids) AND noteDate IS NOT :epochDay")
    suspend fun setDate(ids: List<Long>, epochDay: Long?, now: Long)

    // --- Permanent deletion ---

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteForever(ids: List<Long>)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash(): Int

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun deleteTrashedBefore(threshold: Long): Int
}

/** One ORDER BY handles all four SortOrder values; `id` breaks ties so the order is stable. */
private const val ORDER_BY = """
    CASE WHEN :sort = 'UPDATED_NEWEST_FIRST' THEN updatedAt END DESC,
    CASE WHEN :sort = 'UPDATED_OLDEST_FIRST' THEN updatedAt END ASC,
    CASE WHEN :sort = 'CREATED_NEWEST_FIRST' THEN createdAt END DESC,
    CASE WHEN :sort = 'CREATED_OLDEST_FIRST' THEN createdAt END ASC,
    id DESC"""
