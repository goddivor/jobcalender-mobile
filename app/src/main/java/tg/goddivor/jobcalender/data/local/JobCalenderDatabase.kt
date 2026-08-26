package tg.goddivor.jobcalender.data.local

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverters

@ColumnTypeConverters(Converters::class)
@Database(
    entities = [ApplicationEntity::class, EventEntity::class, PendingWriteEntity::class],
    version = 2,
    exportSchema = true,
    // Version 2 only adds the pending_writes table, which Room can generate on its own. The
    // applications and their events must survive the upgrade: they are the whole point of the app.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class JobCalenderDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
    abstract fun eventDao(): EventDao
    abstract fun pendingWriteDao(): PendingWriteDao

    companion object {
        const val NAME = "jobcalender.db"
    }
}
