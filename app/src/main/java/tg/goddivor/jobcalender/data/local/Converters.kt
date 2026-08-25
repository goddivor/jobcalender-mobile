package tg.goddivor.jobcalender.data.local

import androidx.room3.ColumnTypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Dates and times are stored as ISO text so a snapshot pushed to MongoDB reads the same on both
 * sides, and so a stored row stays legible in a SQLite browser. minSdk 26 carries java.time
 * natively, so no desugaring is involved.
 *
 * Room 3 renamed the annotations: @ColumnTypeConverter and @ColumnTypeConverters, not
 * @TypeConverter and @TypeConverters. The old names no longer exist in androidx.room3.
 *
 * Enums are deliberately absent: Room stores them as their name on its own.
 */
class Converters {
    @ColumnTypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @ColumnTypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @ColumnTypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @ColumnTypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @ColumnTypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @ColumnTypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
