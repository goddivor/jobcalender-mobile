package tg.goddivor.jobcalender.domain.usecase

import tg.goddivor.jobcalender.domain.model.Event
import java.time.LocalDate
import java.time.LocalTime

/**
 * Two events overlap when they fall on the same day and their time ranges intersect.
 *
 * This is the computation the whole app exists for. On 24 August 2026 a Cooperative Horizon meeting at 09:30
 * overlapped an Atelier Numerique test at 10:00, and the meeting was missed because nothing ever
 * put the two side by side.
 *
 * An event with no hour cannot conflict with anything: guessing a time would invent a clash that
 * may not exist, and silence is more honest than a false alarm.
 */
const val DEFAULT_EVENT_MINUTES = 60

data class ConflictReport(
    val eventIds: Set<String>,
    val days: Set<LocalDate>,
) {
    fun involves(event: Event): Boolean = event.id in eventIds
    fun hasConflictOn(day: LocalDate): Boolean = day in days
}

fun detectConflicts(events: List<Event>): ConflictReport {
    val timed = events.filter { it.time != null }
    val conflicting = mutableSetOf<String>()

    timed.groupBy { it.date }.forEach { (_, sameDay) ->
        for (i in sameDay.indices) {
            for (j in i + 1 until sameDay.size) {
                if (overlaps(sameDay[i], sameDay[j])) {
                    conflicting += sameDay[i].id
                    conflicting += sameDay[j].id
                }
            }
        }
    }

    val days = timed.filter { it.id in conflicting }.map { it.date }.toSet()
    return ConflictReport(eventIds = conflicting, days = days)
}

private fun overlaps(first: Event, second: Event): Boolean {
    val firstStart = first.time ?: return false
    val secondStart = second.time ?: return false
    val firstEnd = firstStart.plusMinutes(first.durationMinutes?.toLong() ?: DEFAULT_EVENT_MINUTES.toLong())
    val secondEnd = secondStart.plusMinutes(second.durationMinutes?.toLong() ?: DEFAULT_EVENT_MINUTES.toLong())
    return firstStart < secondEnd && secondStart < firstEnd
}

/** Guards against a duration that would run past midnight producing a wrapped, meaningless end. */
private operator fun LocalTime.compareTo(other: LocalTime): Int = toSecondOfDay().compareTo(other.toSecondOfDay())
