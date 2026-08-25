package tg.goddivor.jobcalender.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.EventWithApplication
import tg.goddivor.jobcalender.domain.usecase.detectConflicts
import tg.goddivor.jobcalender.ui.format.today
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** What a single day cell needs to draw itself. */
data class DayMarks(
    val types: List<EventType>,
    val count: Int,
    val hasConflict: Boolean,
)

data class NextAppointment(
    val entry: EventWithApplication,
    val daysAway: Long,
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.of(2026, 1),
    val today: LocalDate = LocalDate.of(2026, 1, 1),
    val selectedDay: LocalDate = LocalDate.of(2026, 1, 1),
    val marks: Map<LocalDate, DayMarks> = emptyMap(),
    val dayEvents: List<EventWithApplication> = emptyList(),
    val conflictingIds: Set<String> = emptySet(),
    val conflictCountOnSelectedDay: Int = 0,
    val next: NextAppointment? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: EventRepository,
) : ViewModel() {

    private val today = today()
    private val month = MutableStateFlow(YearMonth.from(today))
    private val selectedDay = MutableStateFlow(today)

    private val monthEvents = month.flatMapLatest { shown ->
        repository.between(shown.atDay(1), shown.atEndOfMonth())
    }

    private val upcoming = repository.upcoming(today)

    val state = combine(month, selectedDay, monthEvents, upcoming) { shown, day, events, ahead ->
        val report = detectConflicts(events.map { it.event })

        val marks = events
            .groupBy { it.event.date }
            .mapValues { (date, sameDay) ->
                DayMarks(
                    types = sameDay.map { it.event.type },
                    count = sameDay.size,
                    hasConflict = report.hasConflictOn(date),
                )
            }

        val dayEvents = events
            .filter { it.event.date == day }
            .sortedWith(compareBy({ it.event.time == null }, { it.event.time }))

        // Only an actual appointment counts here. A closing date is something to prepare, not
        // somewhere to be, and putting it under "prochain rendez-vous" would be a lie.
        val nextEntry = ahead.firstOrNull { it.event.type in APPOINTMENT_TYPES }

        CalendarUiState(
            month = shown,
            today = today,
            selectedDay = day,
            marks = marks,
            dayEvents = dayEvents,
            conflictingIds = report.eventIds,
            conflictCountOnSelectedDay = dayEvents.count { report.involves(it.event) },
            next = nextEntry?.let {
                // ChronoUnit, not Period: Period reports the day component alone, turning a date
                // 37 days away into 6 because the month component holds the rest.
                NextAppointment(
                    entry = it,
                    daysAway = ChronoUnit.DAYS.between(today, it.event.date),
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(month = YearMonth.from(today), today = today, selectedDay = today),
    )

    /**
     * Changing month moves the selection into it. The day list is fed by the shown month's events,
     * so a selection left behind in another month would render as "no event", which is a lie: the
     * events are simply not in the window being queried.
     */
    fun showMonth(target: YearMonth) {
        month.update { target }
        selectedDay.update { if (YearMonth.from(today) == target) today else target.atDay(1) }
    }

    fun selectDay(day: LocalDate) {
        selectedDay.update { day }
        month.update { YearMonth.from(day) }
    }

    fun goToToday() {
        month.update { YearMonth.from(today) }
        selectedDay.update { today }
    }

    private companion object {
        val APPOINTMENT_TYPES = setOf(EventType.INTERVIEW, EventType.TEST, EventType.MEETING)
    }
}
