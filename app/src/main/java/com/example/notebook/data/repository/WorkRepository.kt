package com.example.notebook.data.repository

import com.example.notebook.data.database.WorkShiftDao
import com.example.notebook.data.database.toDomain
import com.example.notebook.data.database.toEntity
import com.example.notebook.domain.WorkLog
import com.example.notebook.domain.WorkLogLabels
import com.example.notebook.domain.WorkShift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Worked days (v1.2) and the "Mondieu" note that sums them up.
 * Every change to the shifts rewrites the note, so it always matches the calendar.
 *
 * @param labels words for the note in the current app language (read at each refresh).
 */
class WorkRepository(
    private val dao: WorkShiftDao,
    private val notes: NoteRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val labels: () -> WorkLogLabels,
) {
    val shifts: Flow<List<WorkShift>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    private val noteMutex = Mutex()

    /** Tap on a day: mark it worked with the usual shift time, or unmark it. */
    suspend fun toggle(epochDay: Long) {
        if (dao.get(epochDay) != null) {
            dao.delete(epochDay)
        } else {
            val s = settings.settings.first()
            dao.upsert(WorkShift(epochDay, s.defaultShiftStart, s.defaultShiftEnd).toEntity())
        }
        refreshNote()
    }

    suspend fun saveShift(shift: WorkShift) {
        dao.upsert(shift.toEntity())
        refreshNote()
    }

    suspend fun removeShift(epochDay: Long) {
        dao.delete(epochDay)
        refreshNote()
    }

    /**
     * Rewrites the note with all shifts. The note is found by a fixed uuid, gets the
     * #work tag, and is created on the first worked day.
     */
    suspend fun refreshNote() = noteMutex.withLock {
        val all = dao.getAll().map { it.toDomain() }
        if (all.isEmpty() && notes.getByUuid(NOTE_UUID) == null) return@withLock
        val rate = settings.settings.first().hourlyRateCents
        notes.upsertGenerated(
            uuid = NOTE_UUID,
            title = NOTE_TITLE,
            content = WorkLog.format(all, rate, labels()),
            categoryId = categories.findOrCreate(TAG),
        )
    }

    companion object {
        /** Fixed id of the generated note, so it is updated instead of duplicated (also across backups). */
        const val NOTE_UUID = "aura-work-log-mondieu"
        const val NOTE_TITLE = "Mondieu"
        const val TAG = "work"
    }
}
