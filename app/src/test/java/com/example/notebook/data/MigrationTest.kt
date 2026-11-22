package com.example.notebook.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.notebook.data.database.NotebookDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Database version 1 (v1.0) → 2 (v1.1) must keep every note and category.
 * The helper builds a real version-1 database from app/schemas/…/1.json, fills it,
 * runs the migration and checks the result against 2.json.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NotebookDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun version1To2KeepsNotesAndAddsDateAndOverrides() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL("INSERT INTO categories (id, name, createdAt) VALUES (1, 'Работа', 10)")
            db.execSQL(
                """INSERT INTO notes (id, uuid, title, content, color, categoryId, isPinned, isArchived, deletedAt, createdAt, updatedAt)
                   VALUES (5, 'u-5', 'Старая заметка', 'текст', 'SAGE', 1, 1, 0, NULL, 100, 200)""",
            )
        }

        // Runs the AutoMigration and validates the schema against version 2.
        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT title, content, color, categoryId, createdAt, updatedAt, noteDate FROM notes WHERE id = 5").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals("Старая заметка", c.getString(0))
                assertEquals("текст", c.getString(1))
                assertEquals("SAGE", c.getString(2))
                assertEquals(1L, c.getLong(3))
                assertEquals(100L, c.getLong(4))
                assertEquals(200L, c.getLong(5))
                assertEquals(true, c.isNull(6)) // old notes have no calendar day
            }
            db.query("SELECT COUNT(*) FROM categories").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
            // The new table exists and is empty.
            db.query("SELECT COUNT(*) FROM day_overrides").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        }
    }

    // Full path instead of a bare name: on Windows Room compares the requested path with the
    // configured name and a bare name does not match (test-only workaround).
    private val DB: String = ApplicationProvider.getApplicationContext<Context>()
        .getDatabasePath("migration-test.db").absolutePath
}
