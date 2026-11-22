package com.example.notebook.data

import com.example.notebook.data.backup.BackupException
import com.example.notebook.data.backup.BackupRepository
import com.example.notebook.data.backup.ImportResult
import com.example.notebook.data.database.DayOverrideEntity
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.domain.CategoryResult
import com.example.notebook.domain.NoteColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Export/import rules from docs/SPEC.md, "Экспорт и импорт (JSON)". */
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    private lateinit var source: NotebookDatabase
    private lateinit var target: NotebookDatabase
    private val clock = FakeClock()

    @Before
    fun setUp() {
        source = inMemoryDatabase()
        target = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    private fun notes(db: NotebookDatabase) = NoteRepository(db.noteDao(), clock.function)
    private fun categories(db: NotebookDatabase) = CategoryRepository(db, clock.function)
    private fun backup(db: NotebookDatabase) = BackupRepository(db, contentResolver = null, clock = clock.function)

    /** A source database with every kind of note: colored, pinned, archived, in trash, with category. */
    private suspend fun fillSource() {
        val n = notes(source)
        val work = (categories(source).createCategory("Работа") as CategoryResult.Success).id
        categories(source).createCategory("Пустая категория")
        val a = n.createNote("Покупки", "Молоко", NoteColor.SAGE)
        n.setPinned(listOf(a), true)
        n.createNote("Отчёт", "к пятнице", categoryId = work)
        val archived = n.createNote("Старое", "архив")
        n.archive(listOf(archived))
        val trashed = n.createNote("Мусор", "в корзине")
        n.moveToTrash(listOf(trashed))
    }

    /** Comparable form of all rows: everything except the local database ids. */
    private suspend fun snapshot(db: NotebookDatabase): Set<List<Any?>> {
        val names = db.categoryDao().getAll().associate { it.id to it.name }
        return db.noteDao().getAll().map {
            listOf(it.uuid, it.title, it.content, it.color, it.categoryId?.let(names::get),
                it.isPinned, it.isArchived, it.deletedAt, it.noteDate, it.createdAt, it.updatedAt)
        }.toSet()
    }

    @Test
    fun exportThenImportIntoEmptyDatabaseRestoresEverything() = runTest {
        fillSource()
        val json = backup(source).exportJson()

        val result = backup(target).importJson(json)

        assertEquals(ImportResult(added = 4, updated = 0, skipped = 0), result)
        assertEquals(snapshot(source), snapshot(target))
        assertEquals(
            setOf("Работа", "Пустая категория"),
            target.categoryDao().getAll().map { it.name }.toSet(),
        )
    }

    @Test
    fun importingTheSameFileTwiceCreatesNoDuplicates() = runTest {
        fillSource()
        val json = backup(source).exportJson()
        backup(target).importJson(json)

        val second = backup(target).importJson(json)

        assertEquals(ImportResult(added = 0, updated = 0, skipped = 4), second)
        assertEquals(4, target.noteDao().getAll().size)
        assertEquals(2, target.categoryDao().getAll().size)
    }

    @Test
    fun existingNoteIsUpdatedOnlyWhenFileVersionIsNewer() = runTest {
        val id = notes(source).createNote("t", "старый текст")
        backup(target).importJson(backup(source).exportJson())

        // Edit in the target (newer there): importing the old file must not overwrite it.
        clock.advance(1_000)
        val targetId = target.noteDao().getAll().single().id
        notes(target).updateText(targetId, "t", "правка на телефоне")
        assertEquals(ImportResult(0, 0, 1), backup(target).importJson(backup(source).exportJson()))
        assertEquals("правка на телефоне", target.noteDao().getAll().single().content)

        // Edit in the source later: now the file is newer and wins.
        clock.advance(1_000)
        notes(source).updateText(id, "t", "новый текст")
        assertEquals(ImportResult(0, 1, 0), backup(target).importJson(backup(source).exportJson()))
        assertEquals("новый текст", target.noteDao().getAll().single().content)
    }

    @Test
    fun importNeverDeletesNotesMissingFromTheFile() = runTest {
        notes(target).createNote("только на телефоне", "")
        notes(source).createNote("из файла", "")

        backup(target).importJson(backup(source).exportJson())

        assertEquals(setOf("только на телефоне", "из файла"), target.noteDao().getAll().map { it.title }.toSet())
    }

    @Test
    fun categoryIsMatchedByNameIgnoringCase() = runTest {
        categories(target).createCategory("работа")
        val work = (categories(source).createCategory("РАБОТА") as CategoryResult.Success).id
        notes(source).createNote("x", "", categoryId = work)

        backup(target).importJson(backup(source).exportJson())

        val cats = target.categoryDao().getAll()
        assertEquals(listOf("работа"), cats.map { it.name })
        assertEquals(cats.single().id, target.noteDao().getAll().single().categoryId)
    }

    @Test
    fun brokenFileChangesNothingAndReportsWhy() = runTest {
        notes(target).createNote("моя заметка", "")
        val before = snapshot(target)

        assertReason(BackupException.Reason.NOT_A_BACKUP) { backup(target).importJson("это не json") }
        assertReason(BackupException.Reason.NOT_A_BACKUP) { backup(target).importJson("""{"hello": 1}""") }
        assertReason(BackupException.Reason.NEWER_VERSION) {
            backup(target).importJson("""{"formatVersion": 99, "exportedAt": 0}""")
        }
        // Valid start, broken second note: the first must not be written either (one transaction).
        assertReason(BackupException.Reason.NOT_A_BACKUP) {
            backup(target).importJson(
                """{"formatVersion":1,"exportedAt":0,"notes":[
                   {"uuid":"a","createdAt":1,"updatedAt":1},
                   {"uuid":"b","createdAt":"не число","updatedAt":1}]}""",
            )
        }
        assertEquals(before, snapshot(target))
    }

    @Test
    fun unknownFieldsAndColorsAreTolerated() = runTest {
        val result = backup(target).importJson(
            """{"formatVersion":1,"exportedAt":0,"futureField":true,
               "notes":[{"uuid":"u1","title":"Т","color":"NEON","createdAt":1,"updatedAt":2,"extra":"x"}]}""",
        )
        assertEquals(ImportResult(1, 0, 0), result)
        val note = notes(target).observeActiveNotes(com.example.notebook.domain.SortOrder.UPDATED_NEWEST_FIRST).first().single()
        assertEquals(NoteColor.DEFAULT, note.color)
        assertEquals("u1", note.uuid)
    }

    @Test
    fun exportContainsFormatVersionAndCategoryNames() = runTest {
        fillSource()
        val json = backup(source).exportJson()
        assertTrue(json.contains("\"formatVersion\": 2"))
        assertTrue(json.contains("\"category\": \"Работа\""))
    }

    @Test
    fun noteDatesAndScheduleExceptionsSurviveExportAndImport() = runTest {
        val dated = notes(source).createNote("смена", "", noteDate = 20_413L)
        source.dayOverrideDao().upsert(DayOverrideEntity(20_414L, isWorkDay = false))

        backup(target).importJson(backup(source).exportJson())

        val uuid = source.noteDao().getById(dated)!!.uuid
        assertEquals(20_413L, target.noteDao().getByUuid(uuid)!!.noteDate)
        assertEquals(listOf(DayOverrideEntity(20_414L, false)), target.dayOverrideDao().getAll())
    }

    @Test
    fun formatOneFileFromVersion10StillImports() = runTest {
        val result = backup(target).importJson(
            """{"formatVersion":1,"exportedAt":0,"categories":[{"name":"Работа"}],
               "notes":[{"uuid":"old-1","title":"из 1.0","category":"Работа","createdAt":1,"updatedAt":2}]}""",
        )
        assertEquals(ImportResult(1, 0, 0), result)
        assertEquals(null, target.noteDao().getByUuid("old-1")!!.noteDate)
    }

    private suspend fun assertReason(expected: BackupException.Reason, block: suspend () -> Unit) {
        try {
            block()
            fail("Expected BackupException($expected)")
        } catch (e: BackupException) {
            assertEquals(expected, e.reason)
        }
    }
}
