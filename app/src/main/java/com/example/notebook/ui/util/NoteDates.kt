package com.example.notebook.ui.util

import android.content.Context
import android.text.format.DateFormat
import com.example.notebook.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** How far a date is from today, in calendar days of the given time zone. */
enum class DayDistance { TODAY, YESTERDAY, THIS_YEAR, OLDER }

fun dayDistance(millis: Long, now: Long, zone: TimeZone = TimeZone.getDefault()): DayDistance {
    val date = Calendar.getInstance(zone).apply { timeInMillis = millis }
    val today = Calendar.getInstance(zone).apply { timeInMillis = now }
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

    fun Calendar.sameDayAs(other: Calendar) =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

    return when {
        date.sameDayAs(today) -> DayDistance.TODAY
        date.sameDayAs(yesterday) -> DayDistance.YESTERDAY
        date.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> DayDistance.THIS_YEAR
        else -> DayDistance.OLDER
    }
}

/**
 * Short date for cards and the editor's bottom bar (spec):
 * "14:05" today, "вчера", "12 нояб." this year, "12 нояб. 2025" for earlier years.
 * Dates are stored in UTC and converted to local time only here.
 */
fun formatShortDate(context: Context, millis: Long, now: Long = System.currentTimeMillis()): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val pattern = when (dayDistance(millis, now)) {
        DayDistance.TODAY -> if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        DayDistance.YESTERDAY -> return context.getString(R.string.date_yesterday)
        DayDistance.THIS_YEAR -> DateFormat.getBestDateTimePattern(locale, "dMMM")
        DayDistance.OLDER -> DateFormat.getBestDateTimePattern(locale, "dMMMy")
    }
    return SimpleDateFormat(pattern, locale).format(Date(millis))
}

/** Full date and time, for the "Информация" dialog. */
fun formatFullDate(context: Context, millis: Long): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val skeleton = if (DateFormat.is24HourFormat(context)) "dMMMMyHHmm" else "dMMMMyhmma"
    return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(millis))
}

/**
 * A calendar day (days since 1970-01-01) for display: "14 нояб." in the current year,
 * "14 нояб. 2025" otherwise. Calendar days have no time zone, so none is applied.
 */
fun formatDay(context: Context, epochDay: Long, today: LocalDate = LocalDate.now()): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val day = LocalDate.ofEpochDay(epochDay)
    val skeleton = if (day.year == today.year) "dMMM" else "dMMMy"
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(day)
}
