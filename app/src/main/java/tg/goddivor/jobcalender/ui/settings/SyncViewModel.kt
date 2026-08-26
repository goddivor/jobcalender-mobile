package tg.goddivor.jobcalender.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.BuildConfig
import tg.goddivor.jobcalender.data.remote.ConfigResult
import tg.goddivor.jobcalender.data.remote.SyncEngine
import tg.goddivor.jobcalender.data.remote.SyncResult
import tg.goddivor.jobcalender.data.remote.SyncSettings
import java.time.Instant
import javax.inject.Inject

/** Which sentence the form shows under the key field, if any. */
enum class ConfigError { INVALID_KEY, OFFLINE, SERVER }

data class SyncUiState(
    val configured: Boolean = false,
    val serverUrl: String = "",
    val configuredAt: Instant? = null,
    val configuring: Boolean = false,
    val configError: ConfigError? = null,
    val serverErrorCode: Int = 0,
    val lastSyncAt: Instant? = null,
    val syncOnLaunch: Boolean = true,
    val syncing: Boolean = false,
    val lastResult: SyncResult? = null,
)

private data class FormState(
    val configuring: Boolean = false,
    val error: ConfigError? = null,
    val errorCode: Int = 0,
    val syncing: Boolean = false,
    val result: SyncResult? = null,
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val settings: SyncSettings,
    private val syncEngine: SyncEngine,
) : ViewModel() {

    private val form = MutableStateFlow(FormState())
    private val typedKey = MutableStateFlow("")
    private val typedUrl = MutableStateFlow<String?>(null)

    val key = typedKey

    val state = combine(settings.state, form, typedUrl) { stored, form, url ->
        SyncUiState(
            configured = stored.isConfigured,
            serverUrl = url ?: stored.serverUrl ?: BuildConfig.SYNC_DEFAULT_URL,
            configuredAt = stored.lastSyncAt.takeIf { stored.isConfigured },
            configuring = form.configuring,
            configError = form.error,
            serverErrorCode = form.errorCode,
            lastSyncAt = stored.lastSyncAt,
            syncOnLaunch = stored.syncOnLaunch,
            syncing = form.syncing,
            lastResult = form.result,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    fun setServerUrl(url: String) = typedUrl.update { url }

    fun setKey(value: String) {
        typedKey.update { value }
        form.update { it.copy(error = null) }
    }

    fun fetchConfiguration() {
        if (form.value.configuring || typedKey.value.isBlank()) return
        viewModelScope.launch {
            form.update { it.copy(configuring = true, error = null) }
            val url = typedUrl.value
                ?: settings.state.first().serverUrl
                ?: BuildConfig.SYNC_DEFAULT_URL
            when (val result = syncEngine.configure(url, typedKey.value)) {
                ConfigResult.Success -> {
                    typedKey.update { "" }
                    form.update { it.copy(configuring = false) }
                }
                ConfigResult.InvalidKey ->
                    form.update { it.copy(configuring = false, error = ConfigError.INVALID_KEY) }
                ConfigResult.Offline ->
                    form.update { it.copy(configuring = false, error = ConfigError.OFFLINE) }
                is ConfigResult.ServerError -> form.update {
                    it.copy(configuring = false, error = ConfigError.SERVER, errorCode = result.code)
                }
            }
        }
    }

    fun syncNow() {
        if (form.value.syncing) return
        viewModelScope.launch {
            form.update { it.copy(syncing = true) }
            val result = syncEngine.sync()
            form.update { it.copy(syncing = false, result = result) }
        }
    }

    fun setSyncOnLaunch(enabled: Boolean) =
        viewModelScope.launch { settings.setSyncOnLaunch(enabled) }

    /** Keeps the address and asks for the key again: it was never stored, so it cannot be replayed. */
    fun refreshConfiguration() = clear(keepServerUrl = true)

    fun reset() = clear(keepServerUrl = false)

    private fun clear(keepServerUrl: Boolean) = viewModelScope.launch {
        settings.clearConfig(keepServerUrl)
        typedKey.update { "" }
        if (!keepServerUrl) typedUrl.update { null }
        form.update { FormState() }
    }
}
