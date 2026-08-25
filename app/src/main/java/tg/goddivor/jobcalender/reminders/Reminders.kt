package tg.goddivor.jobcalender.reminders

import java.time.LocalDate
import java.time.LocalTime

/**
 * What the app can remind about, and when. Kept as pure data so the schedule can be reasoned about
 * and tested without an AlarmManager anywhere near it.
 */
enum class ReminderKind { DAY_BEFORE, HOUR_BEFORE, CLOSING }

data class Reminder(
    val eventId: String,
    val kind: ReminderKind,
    val date: LocalDate,
    val time: LocalTime,
    val employer: String,
    val position: String,
    val eventLabel: String,
)

/** The evening before, at a civil hour rather than at midnight. */
val EVENING: LocalTime = LocalTime.of(20, 0)
