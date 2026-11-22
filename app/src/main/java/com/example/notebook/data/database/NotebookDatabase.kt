package com.example.notebook.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.AutoMigration
import androidx.room.migration.Migration

/**
 * The app's SQLite database.
 *
 * Rules (docs/SPEC.md, "Надёжность"):
 * - Every schema change raises [version] and adds a Migration to [MIGRATIONS] plus a test.
 * - exportSchema = true writes app/schemas/<version>.json, which is committed to git.
 * - fallbackToDestructiveMigration is never used: it would silently wipe all notes.
 */
@Database(
    entities = [NoteEntity::class, CategoryEntity::class, DayOverrideEntity::class, WorkShiftEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // v1.1: notes.noteDate (calendar day) + day_overrides table. Only adds, never removes data.
        AutoMigration(from = 1, to = 2),
        // v1.2: work_shifts table (worked days with hours). Only adds.
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class NotebookDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun dayOverrideDao(): DayOverrideDao
    abstract fun workShiftDao(): WorkShiftDao

    companion object {
        const val FILE_NAME = "notebook.db"

        /** Add migrations here, e.g. MIGRATION_1_2, when the version goes up. */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun create(context: Context): NotebookDatabase =
            Room.databaseBuilder(context.applicationContext, NotebookDatabase::class.java, FILE_NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
