package com.example.notebook.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * One worked day (v1.2): the day and the shift time, in minutes after midnight.
 * An end time earlier than the start (22:00–06:00) means the shift ends the next day.
 */
data class WorkShift(
    val epochDay: Long,
    val startMinute: Int,
    val endMinute: Int,
) {
    init {
        require(startMinute in 0 until MINUTES_PER_DAY && endMinute in 0 until MINUTES_PER_DAY) { "0..1439 minutes" }
    }

    /** Length of the shift in minutes; equal start and end counts as a full 24 hours. */
    val minutes: Int
        get() = Math.floorMod(endMinute - startMinute, MINUTES_PER_DAY).let { if (it == 0) MINUTES_PER_DAY else it }

    val month: YearMonth get() = YearMonth.from(LocalDate.ofEpochDay(epochDay))

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

/** Totals of one month: worked days, minutes and pay (in euro cents). */
data class MonthTotal(val month: YearMonth, val days: Int, val minutes: Int, val payCents: Long)

/** Pay for some minutes at an hourly rate in cents, rounded to whole cents. */
fun pay(minutes: Int, hourlyRateCents: Int): Long = Math.round(minutes * hourlyRateCents / 60.0)

fun monthTotal(month: YearMonth, shifts: List<WorkShift>, hourlyRateCents: Int): MonthTotal {
    val inMonth = shifts.filter { it.month == month }
    val minutes = inMonth.sumOf { it.minutes }
    return MonthTotal(month, inMonth.size, minutes, pay(minutes, hourlyRateCents))
}

/** "07:30" */
fun formatTime(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

/** "14" or "13,5" (hours, with the decimal separator of the app language). */
fun formatHours(minutes: Int, decimalSeparator: Char): String {
    val whole = minutes / 60
    val rest = minutes % 60
    if (rest == 0) return whole.toString()
    val fraction = Math.round(rest / 60.0 * 100).toInt().toString().padStart(2, '0').trimEnd('0')
    return "$whole$decimalSeparator$fraction"
}

/** "938 €" or "1 484,50 €" style amount from cents (simple, locale-light formatting). */
fun formatEuro(cents: Long, decimalSeparator: Char, groupSeparator: Char): String {
    val euros = cents / 100
    val rest = (cents % 100).toInt()
    val grouped = euros.toString().reversed().chunked(3).joinToString(groupSeparator.toString()).reversed()
    return if (rest == 0) "$grouped €" else "$grouped$decimalSeparator${rest.toString().padStart(2, '0')} €"
}
