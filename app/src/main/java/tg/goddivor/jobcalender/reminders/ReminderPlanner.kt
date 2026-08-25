package tg.goddivor.jobcalender.reminders

import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.EventWithApplication
import java.time.LocalDateTime

data class ReminderPreferences(
    val dayBefore: Boolean,
    val hourBefore: Boolean,
    val closing: Boolean,
)

/**
 * Turns upcoming events into the reminders that are still worth firing.
 *
 * An event with no hour gets no hour-before reminder: there is nothing to count back from, and
 * inventing a time would wake the user for nothing. A reminder already in the past is dropped
 * rather than fired late, which is how a phone that was off all night behaves sensibly.
 */
fun planReminders(
    events: List<EventWithApplication>,
    preferences: ReminderPreferences,
    now: LocalDateTime,
): List<Reminder> = buildList {
    for (entry in events) {
        val event = entry.event
        val isAppointment = event.type in APPOINTMENT_TYPES
        val isClosing = event.type == EventType.DEADLINE

        if (isAppointment && preferences.dayBefore) {
            add(
                Reminder(
                    eventId = event.id,
                    kind = ReminderKind.DAY_BEFORE,
                    date = event.date.minusDays(1),
                    time = EVENING,
                    employer = entry.application.employer,
                    position = entry.application.position,
                    eventLabel = event.type.name,
                ),
            )
        }

        val hour = event.time
        if (isAppointment && preferences.hourBefore && hour != null) {
            val at = LocalDateTime.of(event.date, hour).minusHours(1)
            add(
                Reminder(
                    eventId = event.id,
                    kind = ReminderKind.HOUR_BEFORE,
                    date = at.toLocalDate(),
                    time = at.toLocalTime(),
                    employer = entry.application.employer,
                    position = entry.application.position,
                    eventLabel = event.type.name,
                ),
            )
        }

        if (isClosing && preferences.closing) {
            add(
                Reminder(
                    eventId = event.id,
                    kind = ReminderKind.CLOSING,
                    date = event.date.minusDays(CLOSING_LEAD_DAYS),
                    time = EVENING,
                    employer = entry.application.employer,
                    position = entry.application.position,
                    eventLabel = event.type.name,
                ),
            )
        }
    }
}.filter { LocalDateTime.of(it.date, it.time).isAfter(now) }

private const val CLOSING_LEAD_DAYS = 3L

private val APPOINTMENT_TYPES = setOf(EventType.INTERVIEW, EventType.TEST, EventType.MEETING)
