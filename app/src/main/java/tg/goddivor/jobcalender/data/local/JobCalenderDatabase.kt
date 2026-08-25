package tg.goddivor.jobcalender.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverters

@ColumnTypeConverters(Converters::class)
@Database(
    entities = [ApplicationEntity::class, EventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class JobCalenderDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
    abstract fun eventDao(): EventDao

    companion object {
        const val NAME = "jobcalender.db"
    }
}
