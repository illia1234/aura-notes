package com.example.notebook.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.notebook.domain.Category

/**
 * Row of the `categories` table.
 *
 * The unique index uses NOCASE, which in SQLite only ignores case for Latin letters.
 * Full case-insensitive uniqueness (including Cyrillic) is checked in CategoryRepository;
 * the index is a safety net against duplicates slipping in some other way.
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val createdAt: Long,
)

/** Query result: a category plus how many visible notes it has. */
data class CategoryWithCount(
    @Embedded val category: CategoryEntity,
    val noteCount: Int,
)

fun CategoryEntity.toDomain(noteCount: Int = 0) = Category(
    id = id,
    name = name,
    createdAt = createdAt,
    noteCount = noteCount,
)

fun CategoryWithCount.toDomain() = category.toDomain(noteCount)
