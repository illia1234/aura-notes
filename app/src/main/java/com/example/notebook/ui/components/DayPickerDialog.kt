package com.example.notebook.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.notebook.R
import java.time.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Material 3 date picker that works with calendar days (days since 1970-01-01).
 * The M3 picker counts in UTC milliseconds at midnight, so a day maps to day × 24 h exactly,
 * with no time-zone shift.
 *
 * @param onClear shown as "Убрать дату" when the note already has a day; null hides it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(
    initialEpochDay: Long?,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initialEpochDay ?: LocalDate.now().toEpochDay()) * MILLIS_PER_DAY,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onPick(it / MILLIS_PER_DAY) } ?: onDismiss() },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            if (onClear != null) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.date_clear)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    ) {
        DatePicker(state = state)
    }
}
