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
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.Status
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ApplicationForm(
    val employer: String = "",
    val position: String = "",
    val reference: String = "",
    val channel: Channel = Channel.EMAIL,
    val status: Status = Status.DRAFT,
    val sentAt: LocalDate? = null,
    val closingDate: LocalDate? = null,
    val folder: String = "",
    val contactName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val note: String = "",
) {
    /** Only the two fields without which a row means nothing. Everything else can arrive later. */
    val isValid: Boolean get() = employer.isNotBlank() && position.isNotBlank()
}

data class ApplicationEditUiState(
    val form: ApplicationForm = ApplicationForm(),
    val isNew: Boolean = true,
    val loaded: Boolean = false,
    val dirty: Boolean = false,
    /** Shown in the delete confirmation: cascading away someone's history deserves a real number. */
    val eventCount: Int = 0,
    val showEmployerError: Boolean = false,
    val showPositionError: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class ApplicationEditViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    private val events: EventRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editedId: String? = savedStateHandle.get<String>(ARG_ID)?.takeIf { it != NEW }
    private val _state = MutableStateFlow(ApplicationEditUiState(isNew = editedId == null))
    val state = _state.asStateFlow()

    private var original: Application? = null

    init {
        viewModelScope.launch {
            val stored = editedId?.let { id -> repository.withEvents(id).first() }
            original = stored?.application
            _state.update {
                it.copy(
                    form = stored?.application?.toForm() ?: ApplicationForm(),
                    isNew = stored == null,
                    eventCount = stored?.events?.size ?: 0,
                    loaded = true,
                )
            }
        }
    }

    fun update(transform: (ApplicationForm) -> ApplicationForm) = _state.update {
        it.copy(form = transform(it.form), dirty = true, showEmployerError = false, showPositionError = false)
    }

    fun save() {
        val form = _state.value.form
        if (!form.isValid) {
            _state.update {
                it.copy(
                    showEmployerError = form.employer.isBlank(),
                    showPositionError = form.position.isBlank(),
                )
            }
            return
        }
        viewModelScope.launch {
            repository.upsert(form.toApplication(original))
            _state.update { it.copy(saved = true, dirty = false) }
        }
    }

    fun delete(onDone: () -> Unit) {
        val target = original ?: return
        viewModelScope.launch {
            repository.delete(target)
            onDone()
        }
    }

    private fun Application.toForm() = ApplicationForm(
        employer = employer,
        position = position,
        reference = reference.orEmpty(),
        channel = channel,
        status = status,
        sentAt = sentAt,
        closingDate = closingDate,
        folder = folder.orEmpty(),
        contactName = contactName.orEmpty(),
        contactEmail = contactEmail.orEmpty(),
        contactPhone = contactPhone.orEmpty(),
        note = note.orEmpty(),
    )

    private fun ApplicationForm.toApplication(existing: Application?) = Application(
        id = existing?.id ?: UUID.randomUUID().toString(),
        employer = employer.trim(),
        position = position.trim(),
        reference = reference.trimOrNull(),
        channel = channel,
        status = status,
        sentAt = sentAt,
        closingDate = closingDate,
        folder = folder.trimOrNull(),
        contactName = contactName.trimOrNull(),
        contactEmail = contactEmail.trimOrNull(),
        contactPhone = contactPhone.trimOrNull(),
        note = note.trimOrNull(),
        // Sync compares snapshots on this field, so every write has to move it.
        updatedAt = Instant.now(),
    )

    private fun String.trimOrNull(): String? = trim().takeIf { it.isNotEmpty() }

    companion object {
        const val ARG_ID = "applicationId"
        const val NEW = "new"
    }
}
