package tg.goddivor.jobcalender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.updates.InstallState
import tg.goddivor.jobcalender.updates.UpdateFlow
import tg.goddivor.jobcalender.ui.theme.AppPalette
import tg.goddivor.jobcalender.ui.theme.ThemeMode
import javax.inject.Inject

data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.DEFAULT,
    val amoled: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SyncSettings,
    private val updates: UpdateFlow,
) : ViewModel() {

    val update = updates.state

    init {
        // Silent on purpose: it says nothing when up to date, offline, or when this exact version
        // was already waved away. Only a genuine newer release raises the dialog.
        updates.checkOnLaunch()
    }

    fun downloadOrInstall() {
        if (updates.state.value.install is InstallState.ReadyToInstall) updates.install() else updates.download()
    }

    fun dismissUpdate() = updates.dismiss()

    /** The install permission is granted in another task; coming back is when to act on it. */
    fun onResumed() = updates.onResumed()

    fun openInstallSettings() = updates.openInstallSettings()

    val theme = settings.state
        .map { stored ->
            ThemeState(
                mode = runCatching { ThemeMode.valueOf(stored.themeMode) }
                    .getOrDefault(ThemeMode.SYSTEM),
                palette = runCatching { AppPalette.valueOf(stored.palette) }
                    .getOrDefault(AppPalette.DEFAULT),
                amoled = stored.amoled,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeState())

    /** Flips once the stored theme has actually been read, which is what the splash waits for. */
    val ready = settings.state
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
