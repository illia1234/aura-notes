package com.example.notebook.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.notebook.data.database.CategoryEntity
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.database.toDomain
import com.example.notebook.domain.Category
import com.example.notebook.domain.CategoryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.util.Locale

/**
 * Categories: create, rename, delete, list with note counts.
 *
 * Name rules (spec): trimmed, not empty, at most 30 characters, unique ignoring case.
 * SQLite's NOCASE only folds Latin letters, so the "ignoring case" check is done here
 * with Kotlin's lowercase(), which also handles Cyrillic ("Работа" == "работа").
 */
class CategoryRepository(
    private val database: NotebookDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val categoryDao = database.categoryDao()

    /** Alphabetical order that is correct for Russian and English alike. */
    private val collator = Collator.getInstance()

    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAllWithCounts().map { rows ->
            rows.map { it.toDomain() }.sortedWith(compareBy(collator) { it.name })
        }

    suspend fun createCategory(rawName: String): CategoryResult {
        val name = rawName.trim()
        validate(name)?.let { return it }
        return writeSafely {
            if (nameTaken(name, exceptId = null)) {
                CategoryResult.DuplicateName
            } else {
                CategoryResult.Success(categoryDao.insert(CategoryEntity(name = name, createdAt = clock())))
            }
        }
    }

    suspend fun renameCategory(id: Long, rawName: String): CategoryResult {
        val name = rawName.trim()
        validate(name)?.let { return it }
        return writeSafely {
            when {
                categoryDao.getById(id) == null -> CategoryResult.NotFound
                nameTaken(name, exceptId = id) -> CategoryResult.DuplicateName
                else -> {
                    categoryDao.rename(id, name)
                    CategoryResult.Success(id)
                }
            }
        }
    }

    /** How many notes (including archive and trash) lose this category if it is deleted. */
    suspend fun countNotesInCategory(id: Long): Int = categoryDao.countAllNotes(id)

    /** Deletes the category; its notes stay, without a category (ON DELETE SET NULL). */
    suspend fun deleteCategory(id: Long) = categoryDao.delete(id)

    private fun validate(name: String): CategoryResult? = when {
        name.isEmpty() -> CategoryResult.EmptyName
        name.length > Category.MAX_NAME_LENGTH -> CategoryResult.TooLong
        else -> null
    }

    private suspend fun nameTaken(name: String, exceptId: Long?): Boolean {
        val key = name.lowercase(Locale.ROOT)
        return categoryDao.getAll().any { it.id != exceptId && it.name.lowercase(Locale.ROOT) == key }
    }

    /**
     * Runs the check and the write in one transaction, so two quick taps cannot both pass
     * the duplicate check. The unique index is the last line of defence.
     */
    private suspend fun writeSafely(block: suspend () -> CategoryResult): CategoryResult =
        try {
            database.withTransaction { block() }
        } catch (e: SQLiteConstraintException) {
            CategoryResult.DuplicateName
        }
}
