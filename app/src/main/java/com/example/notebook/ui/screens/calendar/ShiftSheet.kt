package com.example.notebook.ui.screens.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.notebook.R
import com.example.notebook.domain.WorkShift
import com.example.notebook.domain.formatHours
import com.example.notebook.domain.formatTime
import com.example.notebook.ui.theme.AuraColors
import com.example.notebook.ui.util.formatDay
import java.text.DecimalFormatSymbols
import java.util.Locale

private enum class Editing { START, END }

/**
 * Long press on a day (v1.2): the shift time of that day. Hours are counted automatically
 * (a shift may end after midnight). Saving marks the day as worked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftSheet(
    epochDay: Long,
    current: WorkShift?,
    defaultStart: Int,
    defaultEnd: Int,
    onSave: (WorkShift) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val decimal = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    var start by rememberSaveable { mutableIntStateOf(current?.startMinute ?: defaultStart) }
    var end by rememberSaveable { mutableIntStateOf(current?.endMinute ?: defaultEnd) }
    var editing by rememberSaveable { mutableStateOf<Editing?>(null) }
    val shift = WorkShift(epochDay, start, end)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.shift_title, formatDay(context, epochDay)),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))
            TimeRow(stringResource(R.string.shift_start), start) { editing = Editing.START }
            TimeRow(stringResource(R.string.shift_end), end) { editing = Editing.END }
            Text(
                text = stringResource(R.string.work_hours, formatHours(shift.minutes, decimal)),
                style = MaterialTheme.typography.headlineSmall,
                color = AuraColors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (end <= start) {
                Text(
                    text = stringResource(R.string.shift_overnight),
                    style = MaterialTheme.typography.labelMedium,
                    color = AuraColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current != null) {
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.shift_remove)) }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onSave(shift) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraColors.AccentViolet,
                        contentColor = AuraColors.TextPrimary,
                    ),
                ) { Text(stringResource(R.string.category_save)) }
            }
        }
    }

    editing?.let { which ->
        TimeDialog(
            initialMinute = if (which == Editing.START) start else end,
            onPick = { minute ->
                if (which == Editing.START) start = minute else end = minute
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun TimeRow(label: String, minute: Int, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            Text(formatTime(minute), style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Material 3 clock-face time picker in a dialog, 24-hour format. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initialMinute: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state = state) },
    )
}
