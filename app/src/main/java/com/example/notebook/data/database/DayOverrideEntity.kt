package com.example.notebook.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A manual exception to the shift schedule for one day (v1.1, database version 2):
 * "this day is a shift" or "this day is off", whatever the schedule says.
 * A day without a row simply follows the schedule.
 */
@Entity(tableName = "day_overrides")
data class DayOverrideEntity(
    /** The day, as days since 1970-01-01 (LocalDate.toEpochDay()). */
    @PrimaryKey val epochDay: Long,
    val isWorkDay: Boolean,
)

@Dao
interface DayOverrideDao {
    @Query("SELECT * FROM day_overrides")
    fun observeAll(): Flow<List<DayOverrideEntity>>

    @Query("SELECT * FROM day_overrides ORDER BY epochDay")
    suspend fun getAll(): List<DayOverrideEntity>

    /** Adds or replaces the exception for that day. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: DayOverrideEntity)

    @Query("DELETE FROM day_overrides WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)
}
