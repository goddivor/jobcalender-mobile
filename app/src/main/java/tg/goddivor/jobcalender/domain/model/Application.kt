package tg.goddivor.jobcalender.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * One position at one employer. The same employer can hold several, progressing separately.
 * Almost everything is nullable: an unknown contact or an unsent draft are normal states.
 */
data class Application(
    val id: String,
    val employer: String,
    val position: String,
    val reference: String? = null,
    val channel: Channel,
    val status: Status,
    val sentAt: LocalDate? = null,
    val closingDate: LocalDate? = null,
    /** Name of the folder on the owner's computer. A label, never a path to open. */
    val folder: String? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val note: String? = null,
    val updatedAt: Instant,
)
