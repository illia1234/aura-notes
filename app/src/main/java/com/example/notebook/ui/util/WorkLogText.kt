package com.example.notebook.ui.util

import android.content.Context
import com.example.notebook.R
import com.example.notebook.domain.WorkLogLabels
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Words for the "Mondieu" work-log note in the current app language. */
fun workLogLabels(context: Context): WorkLogLabels {
    val res = context.resources
    val locale = res.configuration.locales[0] ?: Locale.getDefault()
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val monthFormat = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    return WorkLogLabels(
        monthTitle = { month -> monthFormat.format(month).replaceFirstChar { it.titlecase(locale) } },
        hours = { value -> res.getString(R.string.work_hours, value) },
        total = { days, hours, money ->
            res.getString(R.string.work_total, res.getQuantityString(R.plurals.work_shifts, days, days), hours, money)
        },
        footer = res.getString(R.string.work_note_footer),
        decimalSeparator = symbols.decimalSeparator,
        // A regular space instead of the narrow no-break space some locales use, so the note stays plain text.
        groupSeparator = if (symbols.groupingSeparator.isWhitespace()) ' ' else symbols.groupingSeparator,
    )
}
