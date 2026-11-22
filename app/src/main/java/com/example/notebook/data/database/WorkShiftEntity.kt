package com.example.notebook.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.notebook.domain.WorkShift
import kotlinx.coroutines.flow.Flow

/**
 * A worked day with its shift time (v1.2, database version 3). One row per day:
 * a day with a row is "worked" (red in the calendar).
 */
@Entity(tableName = "work_shifts")
data class WorkShiftEntity(
    /** The day, as days since 1970-01-01 (LocalDate.toEpochDay()). */
    @PrimaryKey val epochDay: Long,
    /** Minutes after midnight, 0..1439. End < start means the shift ends the next day. */
    val startMinute: Int,
    val endMinute: Int,
)

fun WorkShiftEntity.toDomain() = WorkShift(epochDay, startMinute, endMinute)

fun WorkShift.toEntity() = WorkShiftEntity(epochDay, startMinute, endMinute)

@Dao
interface WorkShiftDao {
    @Query("SELECT * FROM work_shifts ORDER BY epochDay")
    fun observeAll(): Flow<List<WorkShiftEntity>>

    @Query("SELECT * FROM work_shifts ORDER BY epochDay")
    suspend fun getAll(): List<WorkShiftEntity>

    @Query("SELECT * FROM work_shifts WHERE epochDay = :epochDay")
    suspend fun get(epochDay: Long): WorkShiftEntity?

    /** Adds or replaces the shift of that day. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shift: WorkShiftEntity)

    @Query("DELETE FROM work_shifts WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)
}
