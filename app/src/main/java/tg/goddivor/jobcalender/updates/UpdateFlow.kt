package tg.goddivor.jobcalender.updates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateUiState(
    val checking: Boolean = false,
    val release: ReleaseInfo? = null,
    val checkedAndUpToDate: Boolean = false,
    val install: InstallState = InstallState.Idle,
)

/**
 * One update state for the whole app, because two screens need it: the About screen when the user
 * asks, and the launch check that offers a newer version on its own.
 *
 * Application-scoped rather than per-screen, so a download started from the startup dialog survives
 * navigating away from whatever screen was showing.
 */
@Singleton
class UpdateFlow @Inject constructor(
    private val checker: ReleaseChecker,
    private val installer: ApkInstaller,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(UpdateUiState())
    val state = _state.asStateFlow()

    private var downloadId: Long? = null

    /** Silent: says nothing when up to date, offline, or when this version was waved away. */
    fun checkOnLaunch() {
        scope.launch {
            val release = runCatching { checker.startupUpdate() }.getOrNull() ?: return@launch
            _state.update { it.copy(release = release) }
        }
    }

    /** Explicit: reports the outcome either way, because the user asked. */
    fun checkNow() {
        if (_state.value.checking) return
        scope.launch {
            _state.update { it.copy(checking = true, checkedAndUpToDate = false) }
            val release = runCatching { checker.check(force = true) }.getOrNull()
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
        scope.launch { poll() }
    }

    private suspend fun poll() {
        val id = downloadId ?: return
        while (true) {
            val progress = installer.progress(id)
            _state.update { it.copy(install = progress) }
            when (progress) {
                // Hand it straight to the installer: asking for a second tap once the file is
                // already there only adds a step to a flow the user already committed to.
                is InstallState.ReadyToInstall -> {
                    install()
                    return
                }
                is InstallState.Failed -> return
                else -> delay(POLL_MS)
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

    fun dismiss() {
        val version = _state.value.release?.version ?: return
        scope.launch {
            checker.dismiss(version)
            _state.update { it.copy(release = null, install = InstallState.Idle) }
        }
    }

    private companion object {
        const val POLL_MS = 500L
    }
}
