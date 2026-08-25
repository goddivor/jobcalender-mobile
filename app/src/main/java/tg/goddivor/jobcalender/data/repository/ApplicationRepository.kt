package tg.goddivor.jobcalender.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tg.goddivor.jobcalender.data.local.ApplicationDao
import tg.goddivor.jobcalender.data.local.toDomain
import tg.goddivor.jobcalender.data.local.toEntity
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
) {
    fun all(): Flow<List<Application>> =
        dao.all().map { rows -> rows.map { it.toDomain() } }

    fun byStatus(status: Status): Flow<List<Application>> =
        dao.byStatus(status).map { rows -> rows.map { it.toDomain() } }

    fun withEvents(id: String): Flow<ApplicationWithEvents?> =
        dao.withEvents(id).map { it?.toDomain() }

    fun stale(before: LocalDate): Flow<List<Application>> =
        dao.stale(before).map { rows -> rows.map { it.toDomain() } }

    fun countByStatus(): Flow<List<StatusCount>> =
        dao.countByStatus().map { rows -> rows.map { it.toDomain() } }

    suspend fun count(): Int = dao.count()

    /** The folder name is the join key the jobing MCP will use later. */
    suspend fun findByFolder(folder: String): Application? = dao.findByFolder(folder)?.toDomain()

    suspend fun upsert(application: Application) = dao.upsert(application.toEntity())

    suspend fun upsertAll(applications: List<Application>) =
        dao.upsertAll(applications.map { it.toEntity() })

    suspend fun delete(application: Application) = dao.delete(application.toEntity())
}
