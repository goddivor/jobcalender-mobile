package tg.goddivor.jobcalender.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A dated fact attached to an application, and the calendar's raw material.
 * Times are Lomé time (Africa/Lome, GMT, no DST) and are never converted for display.
 */
data class Event(
    val id: String,
    val applicationId: String,
    val type: EventType,
    val date: LocalDate,
    /** Absent on an acknowledgement, and on an invitation that gave no hour. */
    val time: LocalTime? = null,
    val durationMinutes: Int? = null,
    val mode: EventMode? = null,
    val location: String? = null,
    /** Null while the organiser has not sent it yet, which the UI shows rather than hides. */
    val link: String? = null,
    val outcome: EventOutcome? = null,
    val note: String? = null,
    val updatedAt: Instant,
)

/** An event carrying the application it belongs to, which every calendar row needs. */
data class EventWithApplication(
    val event: Event,
    val application: Application,
)

data class ApplicationWithEvents(
    val application: Application,
    val events: List<Event>,
)

data class StatusCount(
    val status: Status,
    val count: Int,
)
