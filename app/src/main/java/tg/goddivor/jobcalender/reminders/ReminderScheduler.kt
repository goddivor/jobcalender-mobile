package tg.goddivor.jobcalender.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.ui.format.LOME
import tg.goddivor.jobcalender.ui.format.hhmm
import tg.goddivor.jobcalender.ui.format.short
import tg.goddivor.jobcalender.ui.format.today
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Books one exact alarm per reminder.
 *
 * A calendar app may declare USE_EXACT_ALARM, which Android grants at install, unlike
 * SCHEDULE_EXACT_ALARM which has been denied by default since Android 14. The permission is checked
 * anyway: a user or a manufacturer can still take it away, and a silent inexact alarm is better
 * than a crash.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val events: EventRepository,
    private val settings: SyncSettings,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    suspend fun reschedule() {
        cancelAll()

        val stored = settings.state.first()
        val preferences = ReminderPreferences(
            dayBefore = stored.reminderDayBefore,
            hourBefore = stored.reminderHourBefore,
            closing = stored.reminderClosing,
        )
        if (!preferences.dayBefore && !preferences.hourBefore && !preferences.closing) return

        val upcoming = events.upcoming(today()).first()
        val plan = planReminders(upcoming, preferences, LocalDateTime.now(LOME))

        plan.forEach { schedule(it) }
        remember(plan.map { it.requestCode() })
        Log.i(TAG, "Scheduled ${plan.size} reminder(s), exact=${canScheduleExact()}")
    }

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarms.canScheduleExactAlarms() else true

    private fun schedule(reminder: Reminder) {
        val at = LocalDateTime.of(reminder.date, reminder.time)
            .atZone(LOME)
            .toInstant()
            .toEpochMilli()

        val label = if (reminder.kind == ReminderKind.HOUR_BEFORE) {
            reminder.time.plusHours(1).hhmm()
        } else {
            reminder.date.plusDays(1).short()
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminder.requestCode())
            putExtra(ReminderReceiver.EXTRA_KIND, reminder.kind.name)
            putExtra(ReminderReceiver.EXTRA_EMPLOYER, reminder.employer)
            putExtra(ReminderReceiver.EXTRA_POSITION, reminder.position)
            putExtra(ReminderReceiver.EXTRA_WHEN, label)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminder.requestCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        runCatching {
            if (canScheduleExact()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                // Doze can delay this by a good while, but a late reminder still beats none.
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        }
    }

    private fun cancelAll() {
        booked().forEach { code ->
            val pending = PendingIntent.getBroadcast(
                context,
                code,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            )
            pending?.let {
                alarms.cancel(it)
                it.cancel()
            }
        }
    }

    /** Request codes are kept so a rescheduling can cancel exactly what it booked before. */
    private fun booked(): Set<Int> = context
        .getSharedPreferences(STORE, Context.MODE_PRIVATE)
        .getStringSet(KEY, emptySet())
        .orEmpty()
        .mapNotNull(String::toIntOrNull)
        .toSet()

    private fun remember(codes: List<Int>) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, codes.map(Int::toString).toSet())
            .apply()
    }

    private fun Reminder.requestCode(): Int = (eventId + kind.name).hashCode()

    private companion object {
        const val TAG = "JobCalender"
        const val STORE = "reminders"
        const val KEY = "booked"
    }
}
