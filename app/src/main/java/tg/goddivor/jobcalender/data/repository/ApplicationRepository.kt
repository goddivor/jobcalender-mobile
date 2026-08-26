package tg.goddivor.jobcalender.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tg.goddivor.jobcalender.data.local.ApplicationDao
import tg.goddivor.jobcalender.data.local.toDomain
import tg.goddivor.jobcalender.data.local.toEntity
import kotlinx.serialization.json.JsonObject
import tg.goddivor.jobcalender.data.remote.SyncOutbox
import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.ApplicationWithEvents
import tg.goddivor.jobcalender.domain.model.Status
import tg.goddivor.jobcalender.domain.model.StatusCount
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** The only way the rest of the app reads or writes applications. Reads are always Flow. */
@Singleton
class ApplicationRepository @Inject constructor(
    private val dao: ApplicationDao,
    private val outbox: SyncOutbox,
) {
    fun all(): Flow<List<Application>> =
        dao.all().map { rows -> rows.map { it.toDomain() } }

    fun byStatus(status: Status): Flow<List<Application>> =
        dao.byStatus(status).map { rows -> rows.map { it.toDomain() } }

    fun allWithEvents(): Flow<List<ApplicationWithEvents>> =
        dao.allWithEvents().map { rows -> rows.map { it.toDomain() } }

    fun withEvents(id: String): Flow<ApplicationWithEvents?> =
        dao.withEvents(id).map { it?.toDomain() }

    fun stale(before: LocalDate): Flow<List<Application>> =
        dao.stale(before).map { rows -> rows.map { it.toDomain() } }

    fun countByStatus(): Flow<List<StatusCount>> =
        dao.countByStatus().map { rows -> rows.map { it.toDomain() } }

    suspend fun count(): Int = dao.count()

    /** The folder name is the join key the jobing MCP will use later. */
    suspend fun findByFolder(folder: String): Application? = dao.findByFolder(folder)?.toDomain()

    /** A manual edit: written locally, then queued as a single document for the server. */
    suspend fun create(application: Application) {
        dao.upsert(application.toEntity())
        outbox.queueApplicationCreated(application)
    }

    /** Only what changed leaves the device: the rest of the document belongs to whoever wrote it. */
    suspend fun update(application: Application, changes: JsonObject) {
        dao.upsert(application.toEntity())
        outbox.queueApplicationChanged(application.id, changes)
    }

    /** Seed and pull only: filling the local copy must never send it back. */
    suspend fun upsertAllLocally(applications: List<Application>) =
        dao.upsertAll(applications.map { it.toEntity() })

    suspend fun delete(application: Application) {
        dao.delete(application.toEntity())
        outbox.queueApplicationDeleted(application.id)
    }

    suspend fun deleteAllLocally() = dao.deleteAll()
}
