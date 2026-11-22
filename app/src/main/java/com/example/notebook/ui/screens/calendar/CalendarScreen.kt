package com.example.notebook.ui.screens.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notebook.R
import com.example.notebook.domain.DayType
import com.example.notebook.ui.appContainer
import com.example.notebook.ui.components.AuraTopBar
import com.example.notebook.ui.components.GlowFab
import com.example.notebook.ui.components.NoteCard
import com.example.notebook.ui.components.ScreenTitle
import com.example.notebook.ui.components.SectionLabel
import com.example.notebook.ui.components.fadeTopEdge
import com.example.notebook.ui.components.glass
import com.example.notebook.ui.components.noteTransitionKey
import com.example.notebook.ui.components.sharedNoteBounds
import com.example.notebook.ui.components.tabBarInset
import com.example.notebook.ui.theme.AuraColors
import com.example.notebook.ui.theme.LocalAuraPalette
import com.example.notebook.ui.util.formatDay
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Day colors (docs/V1_1_PLAN.md): muted green shift, muted red day off, neutral gray. */
private val WorkDayColor = Color(0xFF2F6B4F)
private val OffDayColor = Color(0xFF8A2E3A)
private val NoneDayColor = Color(0xFF2A2A30)
private val TodayRing = Color(0xFF4FE3F0)

/**
 * "Календарь" tab: month card with shift colors, the notes of the selected day, and
 * "+ Заметка смены" which creates a note on that day.
 */
@Composable
fun CalendarScreen(
    onOpenNote: (Long) -> Unit,
    onCreateNote: (epochDay: Long) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: CalendarViewModel = viewModel {
        val c = appContainer()
        CalendarViewModel(c.noteRepository, c.categoryRepository, c.settingsRepository, c.scheduleRepository, c.applicationScope)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showScheduleSheet by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AuraTopBar(onSearch = onOpenSearch)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = tabBarInset() + 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fadeTopEdge(),
            ) {
                item(key = "title") { ScreenTitle(stringResource(R.string.tab_calendar)) }
                item(key = "month") {
                    MonthCard(
                        state = state,
                        onPrevious = viewModel::previousMonth,
                        onNext = viewModel::nextMonth,
                        onToday = viewModel::goToToday,
                        onSelect = viewModel::select,
                        onToggle = viewModel::toggleDay,
                        onSchedule = { showScheduleSheet = true },
                    )
                }
                item(key = "hint") {
                    Text(
                        text = stringResource(
                            if (state.schedule == null) R.string.calendar_hint_no_schedule else R.string.calendar_hint_long_press,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = AuraColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                item(key = "dayTitle") {
                    SectionLabel(formatDay(context, state.selectedDay), Modifier.padding(top = 8.dp))
                }
                if (state.selectedNotes.isEmpty()) {
                    item(key = "noNotes") {
                        Text(
                            text = stringResource(R.string.calendar_no_notes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuraColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                items(state.selectedNotes, key = { it.note.id }) { item ->
                    NoteCard(
                        note = item.note,
                        categoryName = item.categoryName,
                        isSelected = false,
                        onClick = { onOpenNote(item.note.id) },
                        onLongClick = {},
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        // "+ Заметка смены": label to the left of the glowing button (as in the mockup).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = tabBarInset() + 20.dp),
        ) {
            Text(
                text = stringResource(R.string.calendar_add_shift_note),
                style = MaterialTheme.typography.labelLarge,
                color = AuraColors.TextPrimary,
                textAlign = TextAlign.End,
                // Glass pill so the label stays readable over the cards scrolling underneath.
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(CircleShape)
                    .background(LocalAuraPalette.current.baseBottom.copy(alpha = 0.92f))
                    .glass(CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            GlowFab(
                onClick = { onCreateNote(state.selectedDay) },
                contentDescription = stringResource(R.string.calendar_add_shift_note),
                modifier = Modifier.sharedNoteBounds(noteTransitionKey(null)),
            )
        }
    }

    if (showScheduleSheet) {
        ScheduleSheet(
            current = state.schedule,
            onSave = { schedule ->
                viewModel.saveSchedule(schedule)
                showScheduleSheet = false
            },
            onDismiss = { showScheduleSheet = false },
        )
    }
}

@Composable
private fun MonthCard(
    state: CalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelect: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    onSchedule: () -> Unit,
) {
    val locale = LocalContext.current.resources.configuration.locales[0] ?: Locale.getDefault()
    val monthTitle = DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(state.month)
        .replaceFirstChar { it.titlecase(locale) }

    Column(Modifier.fillMaxWidth().glass().padding(horizontal = 10.dp, vertical = 12.dp)) {
        // ‹ Ноябрь 2026 ›   — tapping the title jumps back to today; the icon opens the schedule.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.calendar_previous), tint = AuraColors.TextPrimary)
            }
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                color = AuraColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .combinedClickable(onClick = onToday, onClickLabel = stringResource(R.string.calendar_today))
                    .padding(vertical = 8.dp),
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.calendar_next), tint = AuraColors.TextPrimary)
            }
            IconButton(onClick = onSchedule) {
                Icon(Icons.Outlined.EditCalendar, stringResource(R.string.schedule_title), tint = AuraColors.TextPrimary)
            }
        }
        Spacer(Modifier.height(4.dp))
        // Weekday names, Monday first, in the app language ("Пн" / "Mon").
        Row {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                )
            }
        }
        state.days.chunked(7).forEach { week ->
            Row {
                week.forEach { cell ->
                    DayCircle(
                        cell = cell,
                        // Tap: change the day (shift / day off) and select it; long press: only select it.
                        onClick = {
                            onSelect(cell.epochDay)
                            onToggle(cell.epochDay)
                        },
                        onLongClick = { onSelect(cell.epochDay) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCircle(cell: DayCell, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val fill = when (cell.type) {
        DayType.WORK -> WorkDayColor
        DayType.OFF -> OffDayColor
        DayType.NONE -> NoneDayColor
    }
    val typeLabel = when (cell.type) {
        DayType.WORK -> stringResource(R.string.day_type_work)
        DayType.OFF -> stringResource(R.string.day_type_off)
        DayType.NONE -> ""
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            // Days of the neighbouring months are visible but dimmed (as in the mockup).
            .alpha(if (cell.inMonth) 1f else 0.35f)
            .clip(CircleShape)
            .background(fill)
            .then(
                when {
                    cell.isToday -> Modifier.border(2.dp, TodayRing, CircleShape)
                    cell.isSelected -> Modifier.border(1.5.dp, AuraColors.TextPrimary, CircleShape)
                    else -> Modifier
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .semantics { contentDescription = "${cell.dayOfMonth} $typeLabel".trim() },
    ) {
        Text(
            text = cell.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (cell.isToday || cell.isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = AuraColors.TextPrimary,
        )
        if (cell.hasNotes) {
            // Small dot: there are notes on this day.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .background(AuraColors.TextPrimary, CircleShape),
            )
        }
        if (cell.isToday && cell.isSelected) {
            // Today and selected: a thin white ring inside the cyan one.
            Box(
                Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .border(1.dp, AuraColors.TextPrimary, CircleShape),
            )
        }
    }
}
