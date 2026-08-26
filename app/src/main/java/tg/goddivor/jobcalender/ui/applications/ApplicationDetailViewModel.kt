package tg.goddivor.jobcalender.ui.applications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.remote.changedFields
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status
import tg.goddivor.jobcalender.domain.usecase.loggedEventTypeFor
import tg.goddivor.jobcalender.domain.usecase.statusNeedsItsOwnDate
import tg.goddivor.jobcalender.ui.format.today
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * The timeline always ends on a "Candidature envoyée" node built from sentAt: the origin of an
 * application is not stored as an event, and a draft has no origin at all.
 */
sealed interface TimelineNode {
    data class Real(val event: Event, val isUpcoming: Boolean) : TimelineNode
    data class Sent(val date: java.time.LocalDate, val channel: Channel) : TimelineNode
}

data class ApplicationDetailUiState(
    val application: Application? = null,
    val timeline: List<TimelineNode> = emptyList(),
    val nextLink: String? = null,
    val awaitingLink: Boolean = false,
    val loaded: Boolean = false,
)

@HiltViewModel
class ApplicationDetailViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    private val events: EventRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val applicationId: String = checkNotNull(savedStateHandle[ARG_ID])

    val state = repository.withEvents(applicationId)
        .map { entry ->
            if (entry == null) return@map ApplicationDetailUiState(loaded = true)

            val today = tg.goddivor.jobcalender.ui.format.today()
            val sorted = entry.events.sortedWith(
                compareByDescending<Event> { it.date }.thenByDescending { it.time },
            )
            val upcoming = sorted.filter { !it.date.isBefore(today) }
            val nextWithLink = upcoming.lastOrNull { it.link != null }
            val nextWithoutLink = upcoming.lastOrNull { it.link == null && it.mode != null }

            ApplicationDetailUiState(
                application = entry.application,
                timeline = buildList {
                    sorted.forEach { add(TimelineNode.Real(it, !it.date.isBefore(today))) }
                    entry.application.sentAt?.let {
                        add(TimelineNode.Sent(it, entry.application.channel))
                    }
                },
                nextLink = nextWithLink?.link,
                awaitingLink = nextWithLink == null && nextWithoutLink != null,
                loaded = true,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ApplicationDetailUiState(),
        )

    /**
     * Records the new status and leaves a trace of it. A status that names a dated appointment gets
     * no invented date: [onNeedsDate] hands the user the event form instead, pre-set to its type.
     */
    fun changeStatus(status: Status, onNeedsDate: (EventType) -> Unit) {
        viewModelScope.launch {
            val current = repository.withEvents(applicationId).first()?.application ?: return@launch
            val updated = current.copy(status = status, updatedAt = Instant.now())
            repository.update(updated, changedFields(current, updated))

            loggedEventTypeFor(status)?.let { type ->
                events.upsert(
                    Event(
                        id = UUID.randomUUID().toString(),
                        applicationId = applicationId,
                        type = type,
                        date = today(),
                        updatedAt = Instant.now(),
                    ),
                )
            }
            if (statusNeedsItsOwnDate(status)) {
                onNeedsDate(if (status == Status.TEST) EventType.TEST else EventType.INTERVIEW)
            }
        }
    }

    companion object {
        const val ARG_ID = "applicationId"
    }
}
