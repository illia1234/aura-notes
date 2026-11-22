package com.example.notebook.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.ScheduleRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.domain.DayType
import com.example.notebook.domain.ShiftSchedule
import com.example.notebook.domain.dayType
import com.example.notebook.domain.toggledOverride
import com.example.notebook.ui.screens.notes.NoteItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** One cell of the month grid. */
data class DayCell(
    val epochDay: Long,
    val dayOfMonth: Int,
    val inMonth: Boolean,
    val type: DayType,
    val hasNotes: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
)

data class CalendarUiState(
    val month: YearMonth,
    /** Always 6 weeks × 7 days, Monday first. */
    val days: List<DayCell> = emptyList(),
    val selectedDay: Long,
    val selectedNotes: List<NoteItem> = emptyList(),
    val schedule: ShiftSchedule? = null,
)

/**
 * "Календарь" tab (v1.1): a month grid colored by the shift schedule (green = shift,
 * red = day off), manual exceptions on long press, and the notes of the selected day.
 *
 * @param today injectable so tests do not depend on the real date.
 */
class CalendarViewModel(
    private val notes: NoteRepository,
    categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val appScope: CoroutineScope,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(today))
    private val selected = MutableStateFlow(today.toEpochDay())

    val state: StateFlow<CalendarUiState> = combine(
        combine(month, selected, ::Pair),
        settings.schedule,
        scheduleRepository.overrides,
        notes.observeDatedNotes(),
        categories.observeCategories(),
    ) { (shownMonth, selectedDay), schedule, overrides, dated, cats ->
        val names = cats.associate { it.id to it.name }
        val daysWithNotes = dated.mapNotNullTo(HashSet()) { it.noteDate }
        CalendarUiState(
            month = shownMonth,
            days = monthGrid(shownMonth).map { date ->
                val day = date.toEpochDay()
                DayCell(
                    epochDay = day,
                    dayOfMonth = date.dayOfMonth,
                    inMonth = YearMonth.from(date) == shownMonth,
                    type = dayType(day, schedule, overrides),
                    hasNotes = day in daysWithNotes,
                    isToday = date == today,
                    isSelected = day == selectedDay,
                )
            },
            selectedDay = selectedDay,
            selectedNotes = dated.filter { it.noteDate == selectedDay }
                .map { NoteItem(it, it.categoryId?.let(names::get)) },
            schedule = schedule,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(month = YearMonth.from(today), selectedDay = today.toEpochDay()),
    )

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    /** Select the day (its notes are shown below); a day of the neighbouring month also switches the month. */
    fun select(epochDay: Long) {
        selected.value = epochDay
        month.value = YearMonth.from(LocalDate.ofEpochDay(epochDay))
    }

    fun goToToday() = select(today.toEpochDay())

    /** Tap on a day: shift ↔ day off over the schedule (see toggledOverride for the full cycle). */
    fun toggleDay(epochDay: Long) {
        appScope.launch {
            val schedule = settings.schedule.first()
            val overrides = scheduleRepository.overrides.first()
            when (val newValue = toggledOverride(epochDay, schedule, overrides)) {
                null -> scheduleRepository.clearOverride(epochDay)
                else -> scheduleRepository.setOverride(epochDay, newValue)
            }
        }
    }

    fun saveSchedule(schedule: ShiftSchedule?) {
        appScope.launch { settings.setSchedule(schedule) }
    }

    companion object {
        /** 42 days covering the month, starting on the Monday on or before the 1st. */
        fun monthGrid(month: YearMonth): List<LocalDate> {
            val first = month.atDay(1)
            val start = first.minusDays((first.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
            return List(42) { start.plusDays(it.toLong()) }
        }
    }
}
