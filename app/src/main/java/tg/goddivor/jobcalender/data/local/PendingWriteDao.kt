package tg.goddivor.jobcalender.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface PendingWriteDao {

    /** Oldest first: an edit and the edit that corrects it must reach the server in that order. */
    @Query("SELECT * FROM pending_writes ORDER BY id")
    suspend fun all(): List<PendingWriteEntity>

    @Query("SELECT COUNT(*) FROM pending_writes")
    suspend fun count(): Int

    @Insert
    suspend fun insert(write: PendingWriteEntity)

    @Query("DELETE FROM pending_writes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_writes SET attempts = attempts + 1 WHERE id = :id")
    suspend fun recordAttempt(id: Long)
}
