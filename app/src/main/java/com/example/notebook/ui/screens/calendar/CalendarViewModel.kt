package com.example.notebook.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notebook.data.repository.CategoryRepository
import com.example.notebook.data.repository.NoteRepository
import com.example.notebook.data.repository.SettingsRepository
import com.example.notebook.data.repository.WorkRepository
import com.example.notebook.domain.MonthTotal
import com.example.notebook.domain.ShiftSchedule
import com.example.notebook.domain.WorkShift
import com.example.notebook.domain.monthTotal
import com.example.notebook.ui.screens.notes.NoteItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    /** Worked shift of this day (red), or null. */
    val shift: WorkShift?,
    /** Not worked yet, but the schedule expects a shift (today or later, after the last entered shift): thin red ring. */
    val isPlanned: Boolean,
    val hasNotes: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
)

data class CalendarUiState(
    val month: YearMonth,
    /** Always 6 weeks × 7 days, Monday first. */
    val days: List<DayCell> = emptyList(),
    val selectedDay: Long,
    val selectedShift: WorkShift? = null,
    val selectedNotes: List<NoteItem> = emptyList(),
    val monthTotal: MonthTotal = MonthTotal(month, 0, 0, 0),
    val schedule: ShiftSchedule? = null,
    val defaultShiftStart: Int = 7 * 60 + 30,
    val defaultShiftEnd: Int = 21 * 60 + 30,
)

/**
 * "Календарь" tab (v1.2): all days gray; a tap marks a worked day (red, usual hours);
 * long press edits the hours. Totals of the shown month below the grid. The schedule only
 * predicts future shifts (ring). Every change also rewrites the "Mondieu" note.
 *
 * @param today injectable so tests do not depend on the real date.
 */
class CalendarViewModel(
    notes: NoteRepository,
    categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val work: WorkRepository,
    private val appScope: CoroutineScope,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(today))
    private val selected = MutableStateFlow(today.toEpochDay())

    val state: StateFlow<CalendarUiState> = combine(
        combine(month, selected, ::Pair),
        settings.settings,
        work.shifts,
        notes.observeDatedNotes(),
        categories.observeCategories(),
    ) { (shownMonth, selectedDay), appSettings, shifts, dated, cats ->
        val names = cats.associate { it.id to it.name }
        val byDay = shifts.associateBy { it.epochDay }
        val daysWithNotes = dated.mapNotNullTo(HashSet()) { it.noteDate }
        val todayDay = today.toEpochDay()
        // Days you already planned by hand (swaps included) win over the cycle:
        // the prediction starts after the last entered shift.
        val predictFrom = maxOf(todayDay, (shifts.maxOfOrNull { it.epochDay } ?: Long.MIN_VALUE) + 1)
        val schedule = appSettings.schedule
        CalendarUiState(
            month = shownMonth,
            days = monthGrid(shownMonth).map { date ->
                val day = date.toEpochDay()
                val shift = byDay[day]
                DayCell(
                    epochDay = day,
                    dayOfMonth = date.dayOfMonth,
                    inMonth = YearMonth.from(date) == shownMonth,
                    shift = shift,
                    isPlanned = shift == null && day >= predictFrom && schedule?.isWorkDay(day) == true,
                    hasNotes = day in daysWithNotes,
                    isToday = date == today,
                    isSelected = day == selectedDay,
                )
            },
            selectedDay = selectedDay,
            selectedShift = byDay[selectedDay],
            selectedNotes = dated.filter { it.noteDate == selectedDay }
                .map { NoteItem(it, it.categoryId?.let(names::get)) },
            monthTotal = monthTotal(shownMonth, shifts, appSettings.hourlyRateCents),
            schedule = schedule,
            defaultShiftStart = appSettings.defaultShiftStart,
            defaultShiftEnd = appSettings.defaultShiftEnd,
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

    /** Tap: worked ↔ not worked (a new worked day gets the usual hours). */
    fun toggleWorked(epochDay: Long) {
        appScope.launch { work.toggle(epochDay) }
    }

    fun saveShift(shift: WorkShift) {
        appScope.launch { work.saveShift(shift) }
    }

    fun removeShift(epochDay: Long) {
        appScope.launch { work.removeShift(epochDay) }
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
