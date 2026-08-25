package tg.goddivor.jobcalender.data.local

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.Relation
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Field names are identical across JSON, Room and MongoDB, so no layer translates a schema. */
@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey val id: String,
    val employer: String,
    val position: String,
    val reference: String? = null,
    val channel: Channel,
    val status: Status,
    val sentAt: LocalDate? = null,
    val closingDate: LocalDate? = null,
    val folder: String? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val note: String? = null,
    /** Drives sync: the snapshot with the newest updatedAt wins, in one direction. */
    val updatedAt: Instant,
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = ApplicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["applicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("applicationId"), Index("date")],
)
data class EventEntity(
    @PrimaryKey val id: String,
    val applicationId: String,
    val type: EventType,
    val date: LocalDate,
    val time: LocalTime? = null,
    val durationMinutes: Int? = null,
    val mode: EventMode? = null,
    val location: String? = null,
    val link: String? = null,
    val outcome: EventOutcome? = null,
    val note: String? = null,
    val updatedAt: Instant,
)

data class ApplicationWithEventsEntity(
    @Embedded val application: ApplicationEntity,
    // Room 3 takes column arrays, so a relation can key on a composite.
    @Relation(parentColumns = ["id"], entityColumns = ["applicationId"])
    val events: List<EventEntity>,
)

data class EventWithApplicationEntity(
    @Embedded val event: EventEntity,
    @Relation(parentColumns = ["applicationId"], entityColumns = ["id"])
    val application: ApplicationEntity,
)

data class StatusCountRow(
    val status: Status,
    val count: Int,
)
