package tg.goddivor.jobcalender.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tg.goddivor.jobcalender.data.local.EventDao
import tg.goddivor.jobcalender.data.local.toDomain
import tg.goddivor.jobcalender.data.local.toEntity
import tg.goddivor.jobcalender.data.remote.SyncOutbox
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventWithApplication
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val dao: EventDao,
    private val outbox: SyncOutbox,
) {
    fun upcoming(from: LocalDate): Flow<List<EventWithApplication>> =
        dao.upcoming(from).map { rows -> rows.map { it.toDomain() } }

    fun between(start: LocalDate, end: LocalDate): Flow<List<EventWithApplication>> =
        dao.between(start, end).map { rows -> rows.map { it.toDomain() } }

    fun onDay(day: LocalDate): Flow<List<EventWithApplication>> =
        dao.onDay(day).map { rows -> rows.map { it.toDomain() } }

    fun forApplication(applicationId: String): Flow<List<Event>> =
        dao.forApplication(applicationId).map { rows -> rows.map { it.toDomain() } }

    suspend fun count(): Int = dao.count()

    /** A manual edit: written locally, then queued as a single document for the server. */
    suspend fun upsert(event: Event) {
        dao.upsert(event.toEntity())
        outbox.queueEventSaved(event)
    }

    /** Seed and pull only: filling the local copy must never send it back. */
    suspend fun upsertAllLocally(events: List<Event>) = dao.upsertAll(events.map { it.toEntity() })

    suspend fun delete(event: Event) {
        dao.delete(event.toEntity())
        outbox.queueEventDeleted(event)
    }
}
