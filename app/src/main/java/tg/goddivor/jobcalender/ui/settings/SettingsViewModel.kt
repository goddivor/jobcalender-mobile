package tg.goddivor.jobcalender.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.BuildConfig
import tg.goddivor.jobcalender.data.remote.SyncEngine
import tg.goddivor.jobcalender.data.remote.SyncResult
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.reminders.ReminderScheduler
import tg.goddivor.jobcalender.ui.theme.ThemeMode
import java.time.Instant
import javax.inject.Inject

data class SettingsUiState(
    val lastSyncAt: Instant? = null,
    val serverConfigured: Boolean = false,
    val syncPossible: Boolean = false,
    val syncOnLaunch: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val dynamicColor: Boolean = false,
    val reminderDayBefore: Boolean = true,
    val reminderHourBefore: Boolean = true,
    val reminderClosing: Boolean = false,
    val exactAlarmsAllowed: Boolean = true,
    val syncing: Boolean = false,
    val lastResult: SyncResult? = null,
    val applicationCount: Int = 0,
    val eventCount: Int = 0,
    val version: String = BuildConfig.VERSION_NAME,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SyncSettings,
    private val syncEngine: SyncEngine,
    private val applications: ApplicationRepository,
    private val events: EventRepository,
    private val reminders: ReminderScheduler,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)
    private val lastResult = MutableStateFlow<SyncResult?>(null)
    private val counts = MutableStateFlow(0 to 0)

    init {
        viewModelScope.launch { counts.update { applications.count() to events.count() } }
    }

    val state = combine(settings.state, syncing, lastResult, counts) { stored, busy, result, count ->
        SettingsUiState(
            lastSyncAt = stored.lastSyncAt,
            serverConfigured = stored.isConfigured,
            // Without a compiled-in key there is no sync at all, which is a working offline app.
            syncPossible = stored.isConfigured || BuildConfig.SYNC_CONFIG_KEY.isNotBlank(),
            syncOnLaunch = stored.syncOnLaunch,
            themeMode = runCatching { ThemeMode.valueOf(stored.themeMode) }
                .getOrDefault(ThemeMode.FOLLOW_SYSTEM),
            dynamicColor = stored.dynamicColor,
            reminderDayBefore = stored.reminderDayBefore,
            reminderHourBefore = stored.reminderHourBefore,
            reminderClosing = stored.reminderClosing,
            exactAlarmsAllowed = reminders.canScheduleExact(),
            syncing = busy,
            lastResult = result,
            applicationCount = count.first,
            eventCount = count.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun syncNow() {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.update { true }
            val result = syncEngine.sync()
            lastResult.update { result }
            counts.update { applications.count() to events.count() }
            syncing.update { false }
        }
    }

    fun setSyncOnLaunch(enabled: Boolean) =
        viewModelScope.launch { settings.setSyncOnLaunch(enabled) }

    fun setThemeMode(mode: ThemeMode) =
        viewModelScope.launch { settings.setThemeMode(mode.name) }

    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { settings.setDynamicColor(enabled) }

    fun setReminderDayBefore(enabled: Boolean) = viewModelScope.launch {
        settings.setReminderDayBefore(enabled)
        reminders.reschedule()
    }

    fun setReminderHourBefore(enabled: Boolean) = viewModelScope.launch {
        settings.setReminderHourBefore(enabled)
        reminders.reschedule()
    }

    fun setReminderClosing(enabled: Boolean) = viewModelScope.launch {
        settings.setReminderClosing(enabled)
        reminders.reschedule()
    }
}
