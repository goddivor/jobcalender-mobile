package tg.goddivor.jobcalender.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.reminders.ReminderScheduler
import javax.inject.Inject

data class RemindersUiState(
    val dayBefore: Boolean = true,
    val hourBefore: Boolean = true,
    val closing: Boolean = false,
    val exactAlarmsAllowed: Boolean = true,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val settings: SyncSettings,
    private val reminders: ReminderScheduler,
) : ViewModel() {

    val state = settings.state
        .map { stored ->
            RemindersUiState(
                dayBefore = stored.reminderDayBefore,
                hourBefore = stored.reminderHourBefore,
                closing = stored.reminderClosing,
                exactAlarmsAllowed = reminders.canScheduleExact(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    fun setDayBefore(enabled: Boolean) = store { settings.setReminderDayBefore(enabled) }

    fun setHourBefore(enabled: Boolean) = store { settings.setReminderHourBefore(enabled) }

    fun setClosing(enabled: Boolean) = store { settings.setReminderClosing(enabled) }

    // Every reminder preference changes which alarms are pending, so each write reschedules.
    private fun store(write: suspend () -> Unit) = viewModelScope.launch {
        write()
        reminders.reschedule()
    }
}
