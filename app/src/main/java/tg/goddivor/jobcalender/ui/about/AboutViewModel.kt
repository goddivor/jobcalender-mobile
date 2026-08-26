package tg.goddivor.jobcalender.ui.about

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.BuildConfig
import tg.goddivor.jobcalender.updates.ReleaseChecker
import tg.goddivor.jobcalender.updates.ReleaseNote
import tg.goddivor.jobcalender.updates.UpdateFlow
import java.time.Instant
import javax.inject.Inject

data class AboutUiState(
    val version: String = BuildConfig.VERSION_NAME,
    val installedAt: Instant? = null,
)

data class WhatsNewUiState(
    val loading: Boolean = true,
    val releases: List<ReleaseNote> = emptyList(),
    val installedVersion: String = BuildConfig.VERSION_NAME,
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updates: UpdateFlow,
    private val releases: ReleaseChecker,
) : ViewModel() {

    private val _state = MutableStateFlow(AboutUiState())
    val state = _state.asStateFlow()

    private val _whatsNew = MutableStateFlow(WhatsNewUiState())
    val whatsNew = _whatsNew.asStateFlow()

    /** The dialog is driven by the shared flow, so a launch offer and a manual check are one thing. */
    val updateState = updates.state

    init {
        // The build date is not carried in the APK; when this version landed on the phone is, and
        // for a release installed from GitHub the two are a day apart at most.
        _state.update { it.copy(installedAt = installedAt()) }
    }

    fun checkForUpdate() = updates.checkNow()

    fun loadWhatsNew() {
        if (!_whatsNew.value.loading && _whatsNew.value.releases.isNotEmpty()) return
        viewModelScope.launch {
            _whatsNew.update { it.copy(loading = true) }
            val history = releases.history()
            _whatsNew.update { it.copy(loading = false, releases = history) }
        }
    }

    fun download() = updates.download()

    fun install() = updates.install()

    fun openInstallSettings() = updates.openInstallSettings()

    fun dismissRelease() = updates.dismiss()

    private fun installedAt(): Instant? = runCatching {
        Instant.ofEpochMilli(
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
                .lastUpdateTime,
        )
    }.getOrNull()
}
