package tg.goddivor.jobcalender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.ui.theme.ThemeMode
import javax.inject.Inject

data class ThemeState(
    val mode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val dynamicColor: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SyncSettings,
) : ViewModel() {
    val theme = settings.state
        .map { stored ->
            ThemeState(
                mode = runCatching { ThemeMode.valueOf(stored.themeMode) }
                    .getOrDefault(ThemeMode.FOLLOW_SYSTEM),
                dynamicColor = stored.dynamicColor,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeState())
}
