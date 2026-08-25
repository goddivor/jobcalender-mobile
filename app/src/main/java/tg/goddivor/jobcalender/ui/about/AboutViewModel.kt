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
import tg.goddivor.jobcalender.updates.ApkInstaller
import tg.goddivor.jobcalender.updates.InstallState
import tg.goddivor.jobcalender.updates.ReleaseChecker
import tg.goddivor.jobcalender.updates.ReleaseInfo
import java.time.Instant
import javax.inject.Inject

data class AboutUiState(
    val version: String = BuildConfig.VERSION_NAME,
    val applicationCount: Int = 0,
    val eventCount: Int = 0,
    val lastSyncAt: Instant? = null,
    val checking: Boolean = false,
    val release: ReleaseInfo? = null,
    val checkedAndUpToDate: Boolean = false,
    val install: InstallState = InstallState.Idle,
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val checker: ReleaseChecker,
    private val installer: ApkInstaller,
    private val settings: SyncSettings,
    private val applications: ApplicationRepository,
    private val events: EventRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AboutUiState())
    val state = _state.asStateFlow()

    private var downloadId: Long? = null

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

    fun checkForUpdate() {
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true, checkedAndUpToDate = false) }
            val release = checker.check(force = true)
            _state.update {
                it.copy(
                    checking = false,
                    release = release?.takeIf { found -> found.isNewerThanInstalled },
                    checkedAndUpToDate = release == null || !release.isNewerThanInstalled,
                )
            }
        }
    }

    fun download() {
        val release = _state.value.release ?: return
        downloadId = installer.enqueue(release)
        if (downloadId == null) {
            _state.update { it.copy(install = InstallState.Failed("download")) }
            return
        }
        viewModelScope.launch { pollDownload() }
    }

    private suspend fun pollDownload() {
        val id = downloadId ?: return
        while (true) {
            val progress = installer.progress(id)
            _state.update { it.copy(install = progress) }
            when (progress) {
                is InstallState.ReadyToInstall, is InstallState.Failed -> return
                else -> kotlinx.coroutines.delay(POLL_MS)
            }
        }
    }

    fun install() {
        val version = _state.value.release?.version ?: return
        if (!installer.canInstall()) {
            _state.update { it.copy(install = InstallState.PermissionNeeded) }
            return
        }
        installer.install(version)
    }

    fun openInstallSettings() = installer.openInstallPermissionSettings()

    fun dismissRelease() {
        val version = _state.value.release?.version ?: return
        viewModelScope.launch {
            checker.dismiss(version)
            _state.update { it.copy(release = null, install = InstallState.Idle) }
        }
    }

    private companion object {
        const val POLL_MS = 500L
    }
}
