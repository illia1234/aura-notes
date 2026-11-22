package com.example.notebook.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** Ready-made shift patterns (docs/V1_1_PLAN.md) plus a custom "N on / M off". */
enum class SchedulePattern(val defaultWork: Int, val defaultOff: Int) {
    /** 2 days on, 2 days off, repeating from the start date. */
    TWO_TWO(2, 2),

    /** Monday–Friday on, weekend off. Does not need a start date. */
    FIVE_TWO(5, 2),

    /** One day (24 h shift) on, three days off. */
    ONE_THREE(1, 3),

    /** Any N days on / M days off, repeating from the start date. */
    CUSTOM(3, 3),
}

/** What a calendar day is, after applying the schedule and manual exceptions. */
enum class DayType { WORK, OFF, NONE }

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
        else -> Math.floorMod(epochDay - startEpochDay, (workDays + offDays).toLong()) < workDays
    }

    companion object {
        const val MAX_DAYS = 14
        private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}

/**
 * The type of a day: a manual exception wins; otherwise the schedule decides;
 * without a schedule the day is neutral.
 */
fun dayType(epochDay: Long, schedule: ShiftSchedule?, overrides: Map<Long, Boolean>): DayType {
    val manual = overrides[epochDay]
    val isWork = manual ?: schedule?.isWorkDay(epochDay) ?: return DayType.NONE
    return if (isWork) DayType.WORK else DayType.OFF
}

/**
 * Long press on a day (spec: "смена ↔ выходной", again = back to the schedule):
 * - with an exception → remove it (the day follows the schedule again);
 * - without one → set the opposite of what the schedule says
 *   (no schedule → mark it as a shift).
 *
 * @return the new exception, or null to remove it.
 */
fun toggledOverride(epochDay: Long, schedule: ShiftSchedule?, overrides: Map<Long, Boolean>): Boolean? =
    if (epochDay in overrides) null else !(schedule?.isWorkDay(epochDay) ?: false)
