package tg.goddivor.jobcalender.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.ApplicationWithEvents
import tg.goddivor.jobcalender.domain.model.Status
import tg.goddivor.jobcalender.ui.format.today
import java.time.LocalDate
import javax.inject.Inject

data class ApplicationRow(
    val application: Application,
    /**
     * When this application last actually moved. Only past events count: a scheduled interview or
     * a closing date still ahead is something to come, not something that happened, and letting
     * either one in would float an untouched draft to the top of the list.
     */
    val lastMovement: LocalDate?,
    val closing: ClosingState,
)

sealed interface ClosingState {
    data object None : ClosingState
    data class Upcoming(val date: LocalDate) : ClosingState
    data object Past : ClosingState
}

data class EmployerGroup(val employer: String, val rows: List<ApplicationRow>)

data class StatusFilter(val status: Status?, val count: Int, val selected: Boolean)

data class ApplicationsUiState(
    val groups: List<EmployerGroup> = emptyList(),
    val filters: List<StatusFilter> = emptyList(),
    val query: String = "",
    val hasAnyApplication: Boolean = false,
)

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    repository: ApplicationRepository,
) : ViewModel() {

    private val selectedStatus = MutableStateFlow<Status?>(null)
    private val query = MutableStateFlow("")

    val state = combine(
        repository.allWithEvents(),
        selectedStatus,
        query,
    ) { applications, status, text ->
        build(applications, status, text)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ApplicationsUiState(),
    )

    fun selectStatus(status: Status?) = selectedStatus.update { status }

    fun search(text: String) = query.update { text }

    private fun build(
        applications: List<ApplicationWithEvents>,
        status: Status?,
        text: String,
    ): ApplicationsUiState {
        val todayDate = today()

        val rows = applications.map { entry ->
            ApplicationRow(
                application = entry.application,
                lastMovement = entry.events
                    .filter { !it.date.isAfter(todayDate) }
                    .maxOfOrNull { it.date }
                    ?: entry.application.sentAt,
                closing = closingState(entry.application.closingDate, todayDate),
            )
        }

        val trimmed = text.trim()
        val visible = rows.filter { row ->
            (status == null || row.application.status == status) &&
                (
                    trimmed.isEmpty() ||
                        row.application.employer.contains(trimmed, ignoreCase = true) ||
                        row.application.position.contains(trimmed, ignoreCase = true)
                    )
        }

        // Groups are ordered by their most recent movement, so what is alive sits at the top.
        val groups = visible
            .groupBy { it.application.employer }
            .map { (employer, groupRows) ->
                EmployerGroup(
                    employer = employer,
                    rows = groupRows.sortedByDescending { it.lastMovement ?: LocalDate.MIN },
                )
            }
            .sortedWith(
                compareByDescending<EmployerGroup> { group ->
                    group.rows.maxOfOrNull { it.lastMovement ?: LocalDate.MIN } ?: LocalDate.MIN
                }.thenBy { it.employer },
            )

        // A status with no application stays selectable: a count of zero is information, not a
        // reason to hide a filter.
        val counts = rows.groupingBy { it.application.status }.eachCount()
        val filters = buildList {
            add(StatusFilter(null, rows.size, status == null))
            Status.entries.forEach { candidate ->
                add(StatusFilter(candidate, counts[candidate] ?: 0, status == candidate))
            }
        }

        return ApplicationsUiState(
            groups = groups,
            filters = filters,
            query = text,
            hasAnyApplication = rows.isNotEmpty(),
        )
    }

    private fun closingState(closingDate: LocalDate?, todayDate: LocalDate): ClosingState = when {
        closingDate == null -> ClosingState.None
        closingDate.isBefore(todayDate) -> ClosingState.Past
        else -> ClosingState.Upcoming(closingDate)
    }
}
