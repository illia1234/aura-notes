package com.example.notebook.ui.screens.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.notebook.R
import com.example.notebook.domain.SchedulePattern
import com.example.notebook.domain.ShiftSchedule
import com.example.notebook.ui.components.DayPickerDialog
import com.example.notebook.ui.theme.AuraColors
import com.example.notebook.ui.util.formatDay
import java.time.LocalDate

/**
 * Shift schedule setup: pattern (2/2, 5/2, 1/3, custom N/M) and, for repeating patterns,
 * the first working day. "Сбросить" removes the schedule (days become neutral).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSheet(
    current: ShiftSchedule?,
    onSave: (ShiftSchedule?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var pattern by rememberSaveable { mutableStateOf(current?.pattern ?: SchedulePattern.TWO_TWO) }
    var work by rememberSaveable { mutableIntStateOf(current?.workDays ?: SchedulePattern.CUSTOM.defaultWork) }
    var off by rememberSaveable { mutableIntStateOf(current?.offDays ?: SchedulePattern.CUSTOM.defaultOff) }
    var start by rememberSaveable { mutableLongStateOf(current?.startEpochDay ?: LocalDate.now().toEpochDay()) }
    var pickStart by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SchedulePattern.entries.forEach { p ->
                    FilterChip(
                        selected = p == pattern,
                        onClick = { pattern = p },
                        label = { Text(stringResource(p.labelRes())) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuraColors.TextPrimary,
                            selectedLabelColor = AuraColors.Background,
                        ),
                    )
                }
            }

            if (pattern == SchedulePattern.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                Stepper(stringResource(R.string.schedule_work_days), work, onChange = { work = it })
                Stepper(stringResource(R.string.schedule_off_days), off, onChange = { off = it })
            }

            if (pattern != SchedulePattern.FIVE_TWO) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.schedule_start),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { pickStart = true }) { Text(formatDay(context, start)) }
                }
                Text(
                    text = stringResource(R.string.schedule_start_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = AuraColors.TextSecondary,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current != null) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.schedule_reset)) }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onSave(
                            ShiftSchedule(
                                pattern = pattern,
                                workDays = if (pattern == SchedulePattern.CUSTOM) work else pattern.defaultWork,
                                offDays = if (pattern == SchedulePattern.CUSTOM) off else pattern.defaultOff,
                                startEpochDay = start,
                            ),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraColors.AccentViolet,
                        contentColor = AuraColors.TextPrimary,
                    ),
                ) { Text(stringResource(R.string.category_save)) }
            }
        }
    }

    if (pickStart) {
        DayPickerDialog(
            initialEpochDay = start,
            onPick = {
                start = it
                pickStart = false
            },
            onDismiss = { pickStart = false },
        )
    }
}

/** "Рабочих дней  − 3 +" with limits 1..14. */
@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - 1).coerceAtLeast(1)) }, enabled = value > 1) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.schedule_less))
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { onChange((value + 1).coerceAtMost(ShiftSchedule.MAX_DAYS)) }, enabled = value < ShiftSchedule.MAX_DAYS) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.schedule_more))
        }
    }
}

private fun SchedulePattern.labelRes(): Int = when (this) {
    SchedulePattern.TWO_TWO -> R.string.schedule_2_2
    SchedulePattern.FIVE_TWO -> R.string.schedule_5_2
    SchedulePattern.ONE_THREE -> R.string.schedule_1_3
    SchedulePattern.CUSTOM -> R.string.schedule_custom
}
