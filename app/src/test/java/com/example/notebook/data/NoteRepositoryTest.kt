package com.example.notebook.data

import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.domain.CategoryResult
import com.example.notebook.domain.NoteColor
import com.example.notebook.domain.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Database and repository rules for notes from docs/SPEC.md. */
@RunWith(RobolectricTestRunner::class)
class NoteRepositoryTest {

    private lateinit var db: NotebookDatabase
    private lateinit var clock: FakeClock
    private lateinit var notes: NoteRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        clock = FakeClock()
        notes = NoteRepository(db.noteDao(), clock.function)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun active(sort: SortOrder = SortOrder.UPDATED_NEWEST_FIRST) =
        notes.observeActiveNotes(sort).first()

    @Test
    fun createdNoteIsStoredWithDatesAndUuid() = runTest {
        val id = notes.createNote("Покупки", "Молоко")
        val note = notes.getNote(id)!!
        assertEquals("Покупки", note.title)
        assertEquals("Молоко", note.content)
        assertEquals(NoteColor.DEFAULT, note.color)
        assertEquals(clock.now, note.createdAt)
        assertEquals(clock.now, note.updatedAt)
        assertEquals(36, note.uuid.length)
        assertEquals(listOf(id), active().map { it.id })
    }

    @Test
    fun eachNoteGetsItsOwnUuid() = runTest {
        val a = notes.getNote(notes.createNote("a", ""))!!
        val b = notes.getNote(notes.createNote("b", ""))!!
        assertNotEquals(a.uuid, b.uuid)
    }

    @Test
    fun editingTextUpdatesOnlyUpdatedAt() = runTest {
        val id = notes.createNote("t", "old")
        val created = clock.now
        clock.advance(5_000)
        notes.updateText(id, "t", "new")
        val note = notes.getNote(id)!!
        assertEquals("new", note.content)
        assertEquals(created, note.createdAt)
        assertEquals(clock.now, note.updatedAt)
    }

    @Test
    fun savingSameTextDoesNotChangeUpdatedAt() = runTest {
        val id = notes.createNote("t", "same")
        val before = notes.getNote(id)!!.updatedAt
        clock.advance(5_000)
        notes.updateText(id, "t", "same")
        assertEquals(before, notes.getNote(id)!!.updatedAt)
    }

    @Test
    fun updatingMissingNoteReturnsFalse() = runTest {
        assertFalse(notes.updateText(42, "x", "y"))
    }

    @Test
    fun pinningAndArchivingDoNotChangeUpdatedAt() = runTest {
        val id = notes.createNote("t", "c")
        val before = notes.getNote(id)!!.updatedAt
        clock.advance(5_000)
        notes.setPinned(listOf(id), true)
        notes.archive(listOf(id))
        notes.unarchive(listOf(id))
        assertEquals(before, notes.getNote(id)!!.updatedAt)
    }

    @Test
    fun colorChangeUpdatesDateOnlyWhenColorDiffers() = runTest {
        val id = notes.createNote("t", "c")
        clock.advance(1_000)
        notes.setColor(listOf(id), NoteColor.DEFAULT) // same color: no change
        assertEquals(clock.now - 1_000, notes.getNote(id)!!.updatedAt)

        notes.setColor(listOf(id), NoteColor.SAGE)
        val note = notes.getNote(id)!!
        assertEquals(NoteColor.SAGE, note.color)
        assertEquals(clock.now, note.updatedAt)
    }

    @Test
    fun pinnedNotesComeFirst() = runTest {
        val old = notes.createNote("old", "")
        clock.advance(1_000)
        val new = notes.createNote("new", "")
        assertEquals(listOf(new, old), active().map { it.id })

        notes.setPinned(listOf(old), true)
        assertEquals(listOf(old, new), active().map { it.id })

        notes.setPinned(listOf(old), false)
        assertEquals(listOf(new, old), active().map { it.id })
    }

    @Test
    fun allFourSortOrders() = runTest {
        // a: created first, edited last. b: created second, never edited.
        val a = notes.createNote("a", "")
        clock.advance(1_000)
        val b = notes.createNote("b", "")
        clock.advance(1_000)
        notes.updateText(a, "a", "edited")

        assertEquals(listOf(a, b), active(SortOrder.UPDATED_NEWEST_FIRST).map { it.id })
        assertEquals(listOf(b, a), active(SortOrder.UPDATED_OLDEST_FIRST).map { it.id })
        assertEquals(listOf(b, a), active(SortOrder.CREATED_NEWEST_FIRST).map { it.id })
        assertEquals(listOf(a, b), active(SortOrder.CREATED_OLDEST_FIRST).map { it.id })
    }

    @Test
    fun archiveHidesFromMainScreenAndUnpins() = runTest {
        val id = notes.createNote("t", "c")
        notes.setPinned(listOf(id), true)
        notes.archive(listOf(id))

        assertTrue(active().isEmpty())
        val archived = notes.observeArchivedNotes(SortOrder.UPDATED_NEWEST_FIRST).first()
        assertEquals(listOf(id), archived.map { it.id })
        assertFalse(archived.single().isPinned)

        notes.unarchive(listOf(id))
        assertEquals(listOf(id), active().map { it.id })
    }

    @Test
    fun trashAndRestore() = runTest {
        val id = notes.createNote("t", "c")
        notes.moveToTrash(listOf(id))

        assertTrue(active().isEmpty())
        val trashed = notes.observeTrash().first().single()
        assertEquals(clock.now, trashed.deletedAt)

        notes.restoreFromTrash(listOf(id))
        assertNull(notes.getNote(id)!!.deletedAt)
        assertEquals(listOf(id), active().map { it.id })
    }

    @Test
    fun archivedNoteInTrashIsNotShownInArchive() = runTest {
        val id = notes.createNote("t", "c")
        notes.archive(listOf(id))
        notes.moveToTrash(listOf(id))
        assertTrue(notes.observeArchivedNotes(SortOrder.UPDATED_NEWEST_FIRST).first().isEmpty())
        assertEquals(1, notes.observeTrash().first().size)
    }

    @Test
    fun purgeRemovesOnlyNotesOlderThan30DaysInTrash() = runTest {
        val old = notes.createNote("old", "")
        val kept = notes.createNote("kept", "")
        notes.moveToTrash(listOf(old))
        clock.advance(NoteRepository.TRASH_RETENTION_MILLIS - 1_000)
        notes.moveToTrash(listOf(kept))
        val alive = notes.createNote("alive", "")
        clock.advance(2_000) // "old" is now just over 30 days in the trash

        assertEquals(1, notes.purgeExpiredTrash())
        assertNull(notes.getNote(old))
        assertNotNull(notes.getNote(kept))
        assertNotNull(notes.getNote(alive))
    }

    @Test
    fun emptyTrashDeletesOnlyTrashedNotes() = runTest {
        val trashed = notes.createNote("x", "")
        val alive = notes.createNote("y", "")
        notes.moveToTrash(listOf(trashed))
        assertEquals(1, notes.emptyTrash())
        assertNull(notes.getNote(trashed))
        assertNotNull(notes.getNote(alive))
    }

    @Test
    fun deleteForeverRemovesRow() = runTest {
        val id = notes.createNote("x", "")
        notes.deleteForever(listOf(id))
        assertNull(notes.getNote(id))
    }

    @Test
    fun deletingCategoryKeepsNotesWithoutCategory() = runTest {
        val categories = CategoryRepository(db, clock.function)
        val catId = (categories.createCategory("Работа") as CategoryResult.Success).id
        val noteId = notes.createNote("t", "c", categoryId = catId)
        assertEquals(1, categories.countNotesInCategory(catId))

        categories.deleteCategory(catId)

        val note = notes.getNote(noteId)
        assertNotNull(note)
        assertNull(note!!.categoryId)
    }

    @Test
    fun unknownColorNameFallsBackToDefault() = runTest {
        val id = notes.createNote("t", "c", color = NoteColor.SAGE)
        db.openHelper.writableDatabase.execSQL("UPDATE notes SET color = 'NEON' WHERE id = $id")
        assertEquals(NoteColor.DEFAULT, notes.getNote(id)!!.color)
    }

    @Test
    fun observeEmitsAfterChange() = runTest {
        val id = notes.createNote("t", "one")
        notes.updateText(id, "t", "two")
        assertEquals("two", notes.observeNote(id).first()!!.content)
    }
}
