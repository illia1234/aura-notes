package com.example.notebook.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /** Categories with the number of notes shown on the main screen (for the side menu). */
    @Query(
        """
        SELECT categories.*, COUNT(notes.id) AS noteCount
        FROM categories
        LEFT JOIN notes ON notes.categoryId = categories.id
            AND notes.isArchived = 0 AND notes.deletedAt IS NULL
        GROUP BY categories.id
        """,
    )
    fun observeAllWithCounts(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** Notes keep existing: the foreign key sets their categoryId to NULL. */
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    /** All notes in the category, including archive and trash (for the delete dialog). */
    @Query("SELECT COUNT(*) FROM notes WHERE categoryId = :id")
    suspend fun countAllNotes(id: Long): Int
}
