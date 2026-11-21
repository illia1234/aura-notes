package com.example.notebook.ui

import com.example.notebook.ui.util.DayDistance
import com.example.notebook.ui.util.dayDistance
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Card date rules: "14:05" today, "вчера", "12 нояб." this year, with year before that. */
class NoteDatesTest {

    private val zone = TimeZone.getTimeZone("Europe/Berlin")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private val now = at(2026, 11, 21, 17, 30)

    @Test
    fun earlierToday() = assertEquals(DayDistance.TODAY, dayDistance(at(2026, 11, 21, 0, 1), now, zone))

    @Test
    fun lateYesterdayIsYesterday() = assertEquals(DayDistance.YESTERDAY, dayDistance(at(2026, 11, 20, 23, 59), now, zone))

    @Test
    fun twoDaysAgoIsThisYear() = assertEquals(DayDistance.THIS_YEAR, dayDistance(at(2026, 11, 19), now, zone))

    @Test
    fun lastYear() = assertEquals(DayDistance.OLDER, dayDistance(at(2025, 11, 12), now, zone))

    @Test
    fun yesterdayAcrossNewYear() {
        val newYearMorning = at(2027, 1, 1, 9, 0)
        assertEquals(DayDistance.YESTERDAY, dayDistance(at(2026, 12, 31, 22, 0), newYearMorning, zone))
    }
}
