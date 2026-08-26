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

    /**
     * Runs at every launch and offers whatever is newer, every time. A refusal closes the dialog for
     * this run only: an update that stayed hidden until the user walked into the settings on their
     * own is an update that never gets installed.
     */
    fun checkOnLaunch() {
        scope.launch {
            val release = runCatching { checker.check(force = true) }.getOrNull() ?: return@launch
            if (!release.isNewerThanInstalled) return@launch
            _state.update {
                // A finished download from an earlier run is reused: the file is already there.
                it.copy(
                    release = release,
                    install = if (installer.downloaded(release.version) != null) {
                        InstallState.ReadyToInstall
                    } else {
                        InstallState.Idle
                    },
                )
            }
        }
    }

    /** Explicit: reports the outcome either way, because the user asked. */
    fun checkNow() {
        if (_state.value.checking) return
        scope.launch {
            _state.update { it.copy(checking = true, checkedAndUpToDate = false) }
            val release = runCatching { checker.check(force = true) }.getOrNull()
            val newer = release?.takeIf { found -> found.isNewerThanInstalled }
            _state.update {
                it.copy(
                    checking = false,
                    release = newer,
                    checkedAndUpToDate = newer == null,
                    install = if (newer != null && installer.downloaded(newer.version) != null) {
                        InstallState.ReadyToInstall
                    } else {
                        InstallState.Idle
                    },
                )
            }
        }
    }

    fun download() {
        val release = _state.value.release ?: return
        // Downloading a file that is already on disk is the same file fetched twice.
        if (installer.downloaded(release.version) != null) {
            _state.update { it.copy(install = InstallState.ReadyToInstall) }
            install()
            return
        }
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
        if (!installer.install(version)) {
            _state.update { it.copy(install = InstallState.Failed("install")) }
        }
    }

    fun openInstallSettings() = installer.openInstallPermissionSettings()

    /**
     * Called when the activity comes back to the front. Granting the install permission happens in
     * the system settings, in another task: without this, the app returns still showing the blocked
     * dialog and the only way forward is to kill it and start again.
     */
    fun onResumed() {
        val state = _state.value
        if (state.release == null) return
        if (state.install !is InstallState.PermissionNeeded || !installer.canInstall()) return
        val ready = installer.downloaded(state.release.version) != null
        _state.update { it.copy(install = if (ready) InstallState.ReadyToInstall else InstallState.Idle) }
        if (ready) install() else download()
    }

    /** Closes the dialog for this run. Nothing is remembered: the next launch offers it again. */
    fun dismiss() {
        _state.update { it.copy(release = null, install = InstallState.Idle) }
    }

    private companion object {
        const val POLL_MS = 500L
    }
}
