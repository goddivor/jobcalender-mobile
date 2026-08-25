package tg.goddivor.jobcalender.data.local

import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.ApplicationWithEvents
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventWithApplication
import tg.goddivor.jobcalender.domain.model.StatusCount

/** The UI only ever sees domain models; entities stop at the repository boundary. */

fun ApplicationEntity.toDomain() = Application(
    id = id,
    employer = employer,
    position = position,
    reference = reference,
    channel = channel,
    status = status,
    sentAt = sentAt,
    closingDate = closingDate,
    folder = folder,
    contactName = contactName,
    contactEmail = contactEmail,
    contactPhone = contactPhone,
    note = note,
    updatedAt = updatedAt,
)

fun Application.toEntity() = ApplicationEntity(
    id = id,
    employer = employer,
    position = position,
    reference = reference,
    channel = channel,
    status = status,
    sentAt = sentAt,
    closingDate = closingDate,
    folder = folder,
    contactName = contactName,
    contactEmail = contactEmail,
    contactPhone = contactPhone,
    note = note,
    updatedAt = updatedAt,
)

fun EventEntity.toDomain() = Event(
    id = id,
    applicationId = applicationId,
    type = type,
    date = date,
    time = time,
    durationMinutes = durationMinutes,
    mode = mode,
    location = location,
    link = link,
    outcome = outcome,
    note = note,
    updatedAt = updatedAt,
)

fun Event.toEntity() = EventEntity(
    id = id,
    applicationId = applicationId,
    type = type,
    date = date,
    time = time,
    durationMinutes = durationMinutes,
    mode = mode,
    location = location,
    link = link,
    outcome = outcome,
    note = note,
    updatedAt = updatedAt,
)

fun EventWithApplicationEntity.toDomain() = EventWithApplication(
    event = event.toDomain(),
    application = application.toDomain(),
)

fun ApplicationWithEventsEntity.toDomain() = ApplicationWithEvents(
    application = application.toDomain(),
    events = events.map { it.toDomain() },
)

fun StatusCountRow.toDomain() = StatusCount(status = status, count = count)
