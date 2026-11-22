package com.example.notebook.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Words used in the work-log note, in the app language. Kept as plain values so the
 * formatting below is pure Kotlin and easy to test.
 */
data class WorkLogLabels(
    val monthTitle: (YearMonth) -> String,
    /** "14 ч" / "14 h" */
    val hours: (String) -> String,
    /** "Итого: 15 смен · 212 ч · 1 484 €" (days, hours text, money text) */
    val total: (Int, String, String) -> String,
    /** Last line, telling the user the note is rewritten automatically. */
    val footer: String,
    val decimalSeparator: Char = ',',
    val groupSeparator: Char = ' ',
)

/**
 * Text of the "Mondieu" note (v1.2), month by month like the user's own list:
 *
 *     Ноябрь 2026
 *     1   07:30–21:30   14 ч
 *     2   07:30–23:30   16 ч
 *     Итого: 2 смены · 30 ч · 210 €
 *     ─────────
 *     Декабрь 2026
 *     …
 */
object WorkLog {

    private const val SEPARATOR = "─────────"

    fun format(shifts: List<WorkShift>, hourlyRateCents: Int, labels: WorkLogLabels): String {
        if (shifts.isEmpty()) return labels.footer
        val byMonth = shifts.sortedBy { it.epochDay }.groupBy { it.month }
        val blocks = byMonth.map { (month, monthShifts) ->
            buildString {
                appendLine(labels.monthTitle(month))
                monthShifts.forEach { shift ->
                    val day = LocalDate.ofEpochDay(shift.epochDay).dayOfMonth.toString().padEnd(3)
                    val time = "${formatTime(shift.startMinute)}–${formatTime(shift.endMinute)}"
                    appendLine("$day $time   ${labels.hours(formatHours(shift.minutes, labels.decimalSeparator))}")
                }
                val total = monthTotal(month, monthShifts, hourlyRateCents)
                append(
                    labels.total(
                        total.days,
                        labels.hours(formatHours(total.minutes, labels.decimalSeparator)),
                        formatEuro(total.payCents, labels.decimalSeparator, labels.groupSeparator),
                    ),
                )
            }
        }
        return blocks.joinToString("\n$SEPARATOR\n") + "\n\n" + labels.footer
    }
}
