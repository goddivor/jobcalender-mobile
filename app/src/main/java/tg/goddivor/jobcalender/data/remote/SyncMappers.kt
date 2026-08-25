package tg.goddivor.jobcalender.data.remote

import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.ApplicationWithEvents
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Enums cross the wire as their own names, so a round trip is lossless and the jobing MCP reads the
 * same vocabulary. An unknown value coming back is a real problem, not something to swallow: it
 * would mean the two sides disagree about the domain.
 */

fun ApplicationWithEvents.toDto() = ApplicationDto(
    id = application.id,
    employer = application.employer,
    position = application.position,
    reference = application.reference,
    channel = application.channel.name,
    status = application.status.name,
    sentAt = application.sentAt?.toString(),
    closingDate = application.closingDate?.toString(),
    folder = application.folder,
    contactName = application.contactName,
    contactEmail = application.contactEmail,
    contactPhone = application.contactPhone,
    note = application.note,
    updatedAt = application.updatedAt.toString(),
    events = events.map { it.toDto() },
)

fun Event.toDto() = EventDto(
    id = id,
    type = type.name,
    date = date.toString(),
    time = time?.toString(),
    durationMinutes = durationMinutes,
    mode = mode?.name,
    location = location,
    link = link,
    outcome = outcome?.name,
    note = note,
    updatedAt = updatedAt.toString(),
)

fun ApplicationDto.toApplication() = Application(
    id = id,
    employer = employer,
    position = position,
    reference = reference,
    channel = Channel.valueOf(channel),
    status = Status.valueOf(status),
    sentAt = sentAt?.let(LocalDate::parse),
    closingDate = closingDate?.let(LocalDate::parse),
    folder = folder,
    contactName = contactName,
    contactEmail = contactEmail,
    contactPhone = contactPhone,
    note = note,
    updatedAt = Instant.parse(updatedAt),
)

fun EventDto.toEvent(applicationId: String) = Event(
    id = id,
    applicationId = applicationId,
    type = EventType.valueOf(type),
    date = LocalDate.parse(date),
    time = time?.let(LocalTime::parse),
    durationMinutes = durationMinutes,
    mode = mode?.let(EventMode::valueOf),
    location = location,
    link = link,
    outcome = outcome?.let(EventOutcome::valueOf),
    note = note,
    updatedAt = Instant.parse(updatedAt),
)
