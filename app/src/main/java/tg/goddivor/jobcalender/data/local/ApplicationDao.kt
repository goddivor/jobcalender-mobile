package tg.goddivor.jobcalender.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import tg.goddivor.jobcalender.domain.model.Status
import java.time.LocalDate

@Dao
interface ApplicationDao {

    @Query("SELECT * FROM applications ORDER BY employer, position")
    fun all(): Flow<List<ApplicationEntity>>

    @Query("SELECT * FROM applications WHERE status = :status ORDER BY employer, position")
    fun byStatus(status: Status): Flow<List<ApplicationEntity>>

    /** Every list screen needs the events too, to know when an application last moved. */
    @Transaction
    @Query("SELECT * FROM applications")
    fun allWithEvents(): Flow<List<ApplicationWithEventsEntity>>

    @Transaction
    @Query("SELECT * FROM applications WHERE id = :id")
    fun withEvents(id: String): Flow<ApplicationWithEventsEntity?>

    /**
     * Sent long ago and still silent. "Silent" means no event at all since [before], which is
     * stricter than looking at the status alone: a status can lag behind reality.
     */
    @Query(
        """
        SELECT * FROM applications
        WHERE status IN ('SENT', 'ACKNOWLEDGED', 'READ', 'NO_REPLY')
          AND (
            SELECT MAX(e.date) FROM events e WHERE e.applicationId = applications.id
          ) IS NULL
          AND sentAt IS NOT NULL AND sentAt < :before
        ORDER BY sentAt
        """,
    )
    fun stale(before: LocalDate): Flow<List<ApplicationEntity>>

    @Query("SELECT status, COUNT(*) AS count FROM applications GROUP BY status")
    fun countByStatus(): Flow<List<StatusCountRow>>

    @Query("SELECT COUNT(*) FROM applications")
    suspend fun count(): Int

    @Query("SELECT * FROM applications WHERE folder = :folder LIMIT 1")
    suspend fun findByFolder(folder: String): ApplicationEntity?

    @Query("DELETE FROM applications")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(application: ApplicationEntity)

    @Upsert
    suspend fun upsertAll(applications: List<ApplicationEntity>)

    @Delete
    suspend fun delete(application: ApplicationEntity)
}
