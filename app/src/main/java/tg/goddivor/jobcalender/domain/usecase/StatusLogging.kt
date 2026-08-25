package tg.goddivor.jobcalender.domain.usecase

import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status

/**
 * Changing a status leaves a trace in the timeline, but only where the date is knowable.
 *
 * An acknowledgement or a rejection happens the moment it is recorded, so today's date is the truth.
 * A test or an interview happens on a date the invitation carries, which the app does not know: the
 * user is sent to the event form instead of having a wrong date invented for them.
 */
fun loggedEventTypeFor(status: Status): EventType? = when (status) {
    Status.ACKNOWLEDGED -> EventType.ACKNOWLEDGEMENT
    Status.READ -> EventType.READ
    Status.SHORTLISTED -> EventType.SHORTLIST
    Status.OFFER -> EventType.OFFER
    Status.REJECTED -> EventType.REJECTION
    Status.TEST, Status.INTERVIEW -> null
    Status.DRAFT, Status.SENT, Status.NO_REPLY -> null
}

/** Statuses that name a dated appointment the app cannot guess. */
fun statusNeedsItsOwnDate(status: Status): Boolean =
    status == Status.TEST || status == Status.INTERVIEW
