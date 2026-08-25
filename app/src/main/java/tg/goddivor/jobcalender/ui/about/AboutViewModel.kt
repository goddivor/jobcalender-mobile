package tg.goddivor.jobcalender.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.BuildConfig
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.updates.UpdateFlow
import java.time.Instant
import javax.inject.Inject

data class AboutUiState(
    val version: String = BuildConfig.VERSION_NAME,
    val applicationCount: Int = 0,
    val eventCount: Int = 0,
    val lastSyncAt: Instant? = null,
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val updates: UpdateFlow,
    private val settings: SyncSettings,
    private val applications: ApplicationRepository,
    private val events: EventRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AboutUiState())
    val state = _state.asStateFlow()

    /** The dialog is driven by the shared flow, so a launch offer and a manual check are one thing. */
    val updateState = updates.state

    init {
        viewModelScope.launch {
            val stored = settings.state.first()
            _state.update {
                it.copy(
                    applicationCount = applications.count(),
                    eventCount = events.count(),
                    lastSyncAt = stored.lastSyncAt,
                )
            }
        }
    }

    fun checkForUpdate() = updates.checkNow()

    fun download() = updates.download()

    fun install() = updates.install()

    fun openInstallSettings() = updates.openInstallSettings()

    fun dismissRelease() = updates.dismiss()
}
