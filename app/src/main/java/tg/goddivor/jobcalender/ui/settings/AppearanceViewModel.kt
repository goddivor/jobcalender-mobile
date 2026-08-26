package tg.goddivor.jobcalender.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.ui.theme.AppPalette
import tg.goddivor.jobcalender.ui.theme.ThemeMode
import javax.inject.Inject

data class AppearanceUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.DEFAULT,
    val amoled: Boolean = false,
)

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settings: SyncSettings,
) : ViewModel() {

    val state = settings.state
        .map { stored ->
            AppearanceUiState(
                themeMode = runCatching { ThemeMode.valueOf(stored.themeMode) }
                    .getOrDefault(ThemeMode.SYSTEM),
                palette = runCatching { AppPalette.valueOf(stored.palette) }
                    .getOrDefault(AppPalette.DEFAULT),
                amoled = stored.amoled,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceUiState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode.name) }

    fun setPalette(palette: AppPalette) = viewModelScope.launch { settings.setPalette(palette.name) }

    fun setAmoled(enabled: Boolean) = viewModelScope.launch { settings.setAmoled(enabled) }
}
