package com.example.notebook.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Shift patterns used to *predict* future shifts in the calendar (v1.2: predicted days get a
 * thin red ring; only days the user marks count as worked).
 */
enum class SchedulePattern(val defaultWork: Int, val defaultOff: Int) {
    /** 14-day rotation: 2 on, 3 off, 2 on, 2 off, 3 on, 2 off (the user's own schedule). */
    ROTATION_2322(2, 2),

    /** 2 days on, 2 days off, repeating from the start date. */
    TWO_TWO(2, 2),

    /** Monday–Friday on, weekend off. Does not need a start date. */
    FIVE_TWO(5, 2),

    /** One day (24 h shift) on, three days off. */
    ONE_THREE(1, 3),

    /** Any N days on / M days off, repeating from the start date. */
    CUSTOM(3, 3),
}

/**
 * A shift schedule. Days are counted as "epoch days" (days since 1970-01-01, see
 * LocalDate.toEpochDay()), which makes cycle arithmetic a simple remainder.
 *
 * @param startEpochDay the first working day of the cycle (ignored for FIVE_TWO).
 */
data class ShiftSchedule(
    val pattern: SchedulePattern,
    val workDays: Int = pattern.defaultWork,
    val offDays: Int = pattern.defaultOff,
    val startEpochDay: Long,
) {
    init {
        require(workDays in 1..MAX_DAYS && offDays in 1..MAX_DAYS) { "1..$MAX_DAYS days on and off" }
    }

    /** Whether the schedule makes this day a working day. Works for days before the start too. */
    fun isWorkDay(epochDay: Long): Boolean = when (pattern) {
        SchedulePattern.FIVE_TWO -> LocalDate.ofEpochDay(epochDay).dayOfWeek !in WEEKEND
        SchedulePattern.ROTATION_2322 ->
            ROTATION[Math.floorMod(epochDay - startEpochDay, ROTATION.size.toLong()).toInt()]
        else -> Math.floorMod(epochDay - startEpochDay, (workDays + offDays).toLong()) < workDays
    }

    companion object {
        const val MAX_DAYS = 14
        private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        /** W W - - - W W - - W W W - - */
        private val ROTATION = booleanArrayOf(
            true, true, false, false, false,
            true, true, false, false,
            true, true, true, false, false,
        )
    }
}
