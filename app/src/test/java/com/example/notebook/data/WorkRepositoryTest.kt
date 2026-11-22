package com.example.notebook.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.notebook.data.database.NotebookDatabase
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.data.repository.WorkRepository
import com.example.notebook.domain.WorkLogLabels
import com.example.notebook.domain.WorkShift
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tap / long press in the calendar and the "Mondieu" note (docs/V1_2_PLAN.md). */
@RunWith(RobolectricTestRunner::class)
class WorkRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var db: NotebookDatabase
    private lateinit var notes: NoteRepository
    private lateinit var categories: CategoryRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        notes = NoteRepository(db.noteDao())
        categories = CategoryRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun kotlinx.coroutines.test.TestScope.work(): WorkRepository {
        val file = folder.newFile("work.preferences_pb").also { it.delete() }
        val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
        val labels = WorkLogLabels(
            monthTitle = { "${it.month.value}/${it.year}" },
            hours = { "$it ч" },
            total = { days, hours, money -> "$days · $hours · $money" },
            footer = "auto",
        )
        return WorkRepository(db.workShiftDao(), notes, categories, settings) { labels }
    }

    private val day = 20_414L // 22 Nov 2025

    @Test
    fun tapMarksDayWithUsualShiftAndTapAgainUnmarks() = runTest {
        val work = work()
        work.toggle(day)
        assertEquals(listOf(WorkShift(day, 7 * 60 + 30, 21 * 60 + 30)), work.shifts.first())

        work.toggle(day)
        assertTrue(work.shifts.first().isEmpty())
    }

    @Test
    fun noteMondieuIsCreatedWithWorkTagAndUpdatedNotDuplicated() = runTest {
        val work = work()
        assertNull(notes.getByUuid(WorkRepository.NOTE_UUID)) // nothing before the first shift

        work.toggle(day)
        val note = notes.getByUuid(WorkRepository.NOTE_UUID)!!
        assertEquals("Mondieu", note.title)
        assertTrue(note.content, note.content.contains("07:30–21:30   14 ч"))
        assertEquals("work", db.categoryDao().getById(note.categoryId!!)!!.name)

        work.saveShift(WorkShift(day, 7 * 60 + 30, 23 * 60 + 30))
        val updated = notes.getByUuid(WorkRepository.NOTE_UUID)!!
        assertTrue(updated.content, updated.content.contains("07:30–23:30   16 ч"))
        assertEquals(1, db.noteDao().getAll().count { it.uuid == WorkRepository.NOTE_UUID })
        assertEquals(1, db.categoryDao().getAll().size) // #work is not created twice
    }

    @Test
    fun existingWorkTagIsReusedIgnoringCase() = runTest {
        val existing = categories.findOrCreate("Work")
        work().toggle(day)
        assertEquals(existing, notes.getByUuid(WorkRepository.NOTE_UUID)!!.categoryId)
    }
}
