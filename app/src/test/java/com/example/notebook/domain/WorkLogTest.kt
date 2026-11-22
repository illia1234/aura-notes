package com.example.notebook.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Work log rules (docs/V1_2_PLAN.md), checked on the user's real October 2026. */
class WorkLogTest {

    private fun day(m: Int, d: Int) = LocalDate.of(2026, m, d).toEpochDay()
    private fun t(h: Int, m: Int = 0) = h * 60 + m

    /** The user's October: two short days, then nine 7:30–21:30 shifts. */
    private val october = listOf(
        WorkShift(day(10, 10), t(9), t(13)),
        WorkShift(day(10, 11), t(13), t(17)),
    ) + listOf(16, 17, 18, 21, 22, 26, 27, 30, 31).map { WorkShift(day(10, it), t(7, 30), t(21, 30)) }

    private val labels = WorkLogLabels(
        monthTitle = { "${it.month.value}/${it.year}" },
        hours = { "$it ч" },
        total = { days, hours, money -> "Итого: $days дн · $hours · $money" },
        footer = "footer",
    )

    @Test
    fun shiftHours() {
        assertEquals(14 * 60, WorkShift(0, t(7, 30), t(21, 30)).minutes)
        assertEquals(16 * 60, WorkShift(0, t(7, 30), t(23, 30)).minutes)
        assertEquals(8 * 60, WorkShift(0, t(22), t(6)).minutes) // ends the next day
    }

    @Test
    fun octoberTotalIs134HoursAnd938Euro() {
        val total = monthTotal(YearMonth.of(2026, 10), october, hourlyRateCents = 700)
        assertEquals(11, total.days)
        assertEquals(134 * 60, total.minutes)
        assertEquals(93_800L, total.payCents)
    }

    @Test
    fun hoursAndMoneyFormatting() {
        assertEquals("14", formatHours(14 * 60, ','))
        assertEquals("13,5", formatHours(13 * 60 + 30, ','))
        assertEquals("7,25", formatHours(7 * 60 + 15, ','))
        assertEquals("938 €", formatEuro(93_800, ',', ' '))
        assertEquals("1 484,50 €", formatEuro(148_450, ',', ' '))
    }

    @Test
    fun noteListsEveryDayAndTotalsPerMonth() {
        val november = listOf(WorkShift(day(11, 2), t(7, 30), t(23, 30)))
        val text = WorkLog.format(october + november, 700, labels)

        assertTrue(text, text.startsWith("10/2026\n"))
        assertTrue(text, text.contains("10  09:00–13:00   4 ч"))
        assertTrue(text, text.contains("31  07:30–21:30   14 ч"))
        assertTrue(text, text.contains("Итого: 11 дн · 134 ч · 938 €"))
        assertTrue(text, text.contains("11/2026\n2   07:30–23:30   16 ч"))
        assertTrue(text, text.contains("Итого: 1 дн · 16 ч · 112 €"))
        assertTrue(text, text.endsWith("footer"))
        // October comes before November.
        assertTrue(text.indexOf("10/2026") < text.indexOf("11/2026"))
    }
}
