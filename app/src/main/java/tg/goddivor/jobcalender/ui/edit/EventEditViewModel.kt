package tg.goddivor.jobcalender.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.reminders.ReminderScheduler
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.ui.format.today
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class EventForm(
    val type: EventType = EventType.INTERVIEW,
    val date: LocalDate = LocalDate.of(2026, 1, 1),
    val time: LocalTime? = null,
    val durationMinutes: String = "",
    val mode: EventMode? = null,
    val location: String = "",
    val link: String = "",
    val outcome: EventOutcome? = null,
    val note: String = "",
)

data class EventEditUiState(
    val form: EventForm = EventForm(),
    val isNew: Boolean = true,
    val loaded: Boolean = false,
    val dirty: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class EventEditViewModel @Inject constructor(
    private val repository: EventRepository,
    private val reminders: ReminderScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val applicationId: String = checkNotNull(savedStateHandle[ARG_APPLICATION_ID])
    private val editedId: String? = savedStateHandle.get<String>(ARG_EVENT_ID)?.takeIf { it != NEW }
    private val suggestedType: EventType? =
        savedStateHandle.get<String>(ARG_TYPE)?.takeIf { it != NONE }?.let(EventType::valueOf)

    private val _state = MutableStateFlow(EventEditUiState())
    val state = _state.asStateFlow()

    private var original: Event? = null

    init {
        viewModelScope.launch {
            val existing = editedId?.let { id ->
                repository.forApplication(applicationId).first().firstOrNull { it.id == id }
            }
            original = existing
            _state.update {
                it.copy(
                    form = existing?.toForm()
                        ?: EventForm(type = suggestedType ?: EventType.INTERVIEW, date = today()),
                    isNew = existing == null,
                    loaded = true,
                )
            }
        }
    }

    fun update(transform: (EventForm) -> EventForm) = _state.update {
        it.copy(form = transform(it.form), dirty = true)
    }

    fun save() {
        viewModelScope.launch {
            repository.upsert(_state.value.form.toEvent(original))
            // A moved appointment needs its alarms moved with it.
            runCatching { reminders.reschedule() }
            _state.update { it.copy(saved = true, dirty = false) }
        }
    }

    fun delete(onDone: () -> Unit) {
        val target = original ?: return
        viewModelScope.launch {
            repository.delete(target)
            runCatching { reminders.reschedule() }
            onDone()
        }
    }

    private fun Event.toForm() = EventForm(
        type = type,
        date = date,
        time = time,
        durationMinutes = durationMinutes?.toString().orEmpty(),
        mode = mode,
        location = location.orEmpty(),
        link = link.orEmpty(),
        outcome = outcome,
        note = note.orEmpty(),
    )

    private fun EventForm.toEvent(existing: Event?) = Event(
        id = existing?.id ?: UUID.randomUUID().toString(),
        applicationId = applicationId,
        type = type,
        date = date,
        time = time,
        // A blank duration stays null rather than becoming zero: absent and "no time at all" are
        // different things, and the conflict rule reads null as "assume an hour".
        durationMinutes = durationMinutes.trim().toIntOrNull()?.takeIf { it > 0 },
        mode = mode,
        location = location.trimOrNull(),
        link = link.trimOrNull(),
        outcome = outcome,
        note = note.trimOrNull(),
        updatedAt = Instant.now(),
    )

    private fun String.trimOrNull(): String? = trim().takeIf { it.isNotEmpty() }

    companion object {
        const val ARG_APPLICATION_ID = "applicationId"
        const val ARG_EVENT_ID = "eventId"
        const val ARG_TYPE = "eventType"
        const val NEW = "new"
        const val NONE = "none"
    }
}
