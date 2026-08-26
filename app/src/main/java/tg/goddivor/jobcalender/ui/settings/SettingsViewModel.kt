package tg.goddivor.jobcalender.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tg.goddivor.jobcalender.BuildConfig
import tg.goddivor.jobcalender.data.remote.SyncSettings
import java.time.Instant
import javax.inject.Inject

data class SettingsUiState(
    val syncConfigured: Boolean = false,
    val lastSyncAt: Instant? = null,
    val version: String = BuildConfig.VERSION_NAME,
)

/** The root settles nothing: it only reports what each page holds. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    settings: SyncSettings,
) : ViewModel() {

    val state = settings.state
        .map { stored ->
            SettingsUiState(
                syncConfigured = stored.isConfigured,
                lastSyncAt = stored.lastSyncAt,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())
}
