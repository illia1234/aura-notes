package com.example.notebook.domain

import com.example.notebook.ui.screens.calendar.CalendarViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Shift schedule rules from docs/V1_1_PLAN.md. */
class ShiftScheduleTest {

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()
    private val start = day(2026, 11, 2)

    private fun pattern(schedule: ShiftSchedule, from: Long, count: Int) =
        (0 until count).joinToString("") { if (schedule.isWorkDay(from + it)) "W" else "-" }

    @Test
    fun twoTwoRepeatsFromStart() {
        assertEquals("WW--WW--", pattern(ShiftSchedule(SchedulePattern.TWO_TWO, startEpochDay = start), start, 8))
    }

    @Test
    fun oneThreeIsOneShiftThenThreeDaysOff() {
        assertEquals("W---W---", pattern(ShiftSchedule(SchedulePattern.ONE_THREE, startEpochDay = start), start, 8))
    }

    @Test
    fun customUsesItsOwnNumbers() {
        val s = ShiftSchedule(SchedulePattern.CUSTOM, workDays = 3, offDays = 1, startEpochDay = start)
        assertEquals("WWW-WWW-", pattern(s, start, 8))
    }

    @Test
    fun daysBeforeTheStartContinueTheCycleBackwards() {
        val s = ShiftSchedule(SchedulePattern.TWO_TWO, startEpochDay = start)
        // The 4 days before the start are "WW--" too, so the cycle looks seamless.
        assertEquals("WW--", pattern(s, start - 4, 4))
    }

    @Test
    fun fiveTwoFollowsWeekdaysRegardlessOfStart() {
        val s = ShiftSchedule(SchedulePattern.FIVE_TWO, startEpochDay = 0)
        val monday = day(2026, 11, 23)
        assertEquals(DayOfWeek.MONDAY, LocalDate.ofEpochDay(monday).dayOfWeek)
        assertEquals("WWWWW--", pattern(s, monday, 7))
    }

    @Test
    fun manualOverrideWinsOverSchedule() {
        val s = ShiftSchedule(SchedulePattern.TWO_TWO, startEpochDay = start)
        assertEquals(DayType.WORK, dayType(start, s, emptyMap()))
        assertEquals(DayType.OFF, dayType(start, s, mapOf(start to false)))
        assertEquals(DayType.WORK, dayType(start + 2, s, mapOf(start + 2 to true)))
    }

    @Test
    fun withoutScheduleDaysAreNeutralUnlessMarked() {
        assertEquals(DayType.NONE, dayType(start, null, emptyMap()))
        assertEquals(DayType.WORK, dayType(start, null, mapOf(start to true)))
    }

    @Test
    fun longPressFlipsTheScheduledTypeAndSecondPressReturnsToSchedule() {
        val s = ShiftSchedule(SchedulePattern.TWO_TWO, startEpochDay = start)
        assertFalse(toggledOverride(start, s, emptyMap())!!) // work day → day off
        assertTrue(toggledOverride(start + 2, s, emptyMap())!!) // day off → shift
        assertNull(toggledOverride(start, s, mapOf(start to false))) // back to the schedule
        assertTrue(toggledOverride(start, null, emptyMap())!!) // no schedule → shift
    }

    @Test
    fun monthGridStartsOnMondayAndHasSixWeeks() {
        val grid = CalendarViewModel.monthGrid(YearMonth.of(2026, 11))
        assertEquals(42, grid.size)
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        // November 2026 starts on a Sunday, so the grid begins on Monday 26 October.
        assertEquals(LocalDate.of(2026, 10, 26), grid.first())
        assertTrue(LocalDate.of(2026, 11, 30) in grid)
    }
}
