package com.example.notebook.data.repository

import com.example.notebook.data.database.DayOverrideDao
import com.example.notebook.data.database.DayOverrideEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manual exceptions to the shift schedule ("this day is a shift / is off").
 * The schedule pattern itself is a setting (SettingsRepository); exceptions are data,
 * so they live in the database and go into backups.
 */
class ScheduleRepository(private val dao: DayOverrideDao) {

    /** epochDay → isWorkDay for every day that has an exception. */
    val overrides: Flow<Map<Long, Boolean>> =
        dao.observeAll().map { rows -> rows.associate { it.epochDay to it.isWorkDay } }

    suspend fun setOverride(epochDay: Long, isWorkDay: Boolean) = dao.upsert(DayOverrideEntity(epochDay, isWorkDay))

    suspend fun clearOverride(epochDay: Long) = dao.delete(epochDay)
}
