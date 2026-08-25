package tg.goddivor.jobcalender.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EventDao {

    /** The next appointment first: this answers the question the user asks most often. */
    @Transaction
    @Query("SELECT * FROM events WHERE date >= :from ORDER BY date, time IS NULL, time")
    fun upcoming(from: LocalDate): Flow<List<EventWithApplicationEntity>>

    @Transaction
    @Query("SELECT * FROM events WHERE date BETWEEN :start AND :end ORDER BY date, time IS NULL, time")
    fun between(start: LocalDate, end: LocalDate): Flow<List<EventWithApplicationEntity>>

    @Transaction
    @Query("SELECT * FROM events WHERE date = :day ORDER BY time IS NULL, time")
    fun onDay(day: LocalDate): Flow<List<EventWithApplicationEntity>>

    @Query("SELECT * FROM events WHERE applicationId = :applicationId ORDER BY date DESC, time DESC")
    fun forApplication(applicationId: String): Flow<List<EventEntity>>

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Delete
    suspend fun delete(event: EventEntity)
}
