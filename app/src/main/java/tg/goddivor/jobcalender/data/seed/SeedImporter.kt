package tg.goddivor.jobcalender.data.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the owner's real 25 applications on first launch.
 *
 * Idempotence comes from deterministic identifiers rather than from a lookup: an application is
 * keyed on its folder name, an event on its folder, type, date and hour. Re-running the import
 * therefore upserts the same rows instead of duplicating them. The folder is also the join key the
 * jobing MCP will use later, which is why it, and not a random UUID, anchors the identity.
 */
@Singleton
class SeedImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val applications: ApplicationRepository,
    private val events: EventRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns the number of applications and events written, or null when nothing was needed. */
    suspend fun importIfEmpty(): ImportReport? {
        if (applications.count() > 0) return null
        return import()
    }

    suspend fun import(): ImportReport {
        val payload = context.assets.open(ASSET_NAME).use { it.readBytes().decodeToString() }
        val seed = json.decodeFromString<SeedFile>(payload)
        val now = Instant.now()

        val parsedApplications = mutableListOf<Application>()
        val parsedEvents = mutableListOf<Event>()

        for (entry in seed.candidatures) {
            val applicationId = idFor(entry.dossier)
            parsedApplications += Application(
                id = applicationId,
                employer = entry.employeur,
                position = entry.poste,
                reference = entry.reference,
                channel = channelOf(entry.canal),
                status = statusOf(entry.statut),
                sentAt = entry.envoyeeLe?.let(LocalDate::parse),
                closingDate = entry.dateCloture?.let(LocalDate::parse),
                folder = entry.dossier,
                contactName = entry.contact?.nom,
                contactEmail = entry.contact?.email,
                contactPhone = entry.contact?.whatsapp,
                note = entry.note,
                updatedAt = now,
            )

            for (event in entry.evenements) {
                parsedEvents += Event(
                    id = idFor("${entry.dossier}|${event.type}|${event.date}|${event.heure.orEmpty()}"),
                    applicationId = applicationId,
                    type = eventTypeOf(event.type),
                    date = LocalDate.parse(event.date),
                    time = event.heure?.let(LocalTime::parse),
                    durationMinutes = event.dureeMin,
                    mode = eventModeOf(event.mode),
                    location = event.lieu,
                    link = event.lien,
                    outcome = eventOutcomeOf(event.statut),
                    note = event.note,
                    updatedAt = now,
                )
            }

            // A closing date lives on the application, not among its events, so it would never
            // reach the calendar on its own. Derive it once, here, rather than in every screen.
            entry.dateCloture?.let { closing ->
                parsedEvents += Event(
                    id = idFor("${entry.dossier}|deadline|$closing"),
                    applicationId = applicationId,
                    type = EventType.DEADLINE,
                    date = LocalDate.parse(closing),
                    updatedAt = now,
                )
            }
        }

        applications.upsertAllLocally(parsedApplications)
        events.upsertAllLocally(parsedEvents)

        return ImportReport(
            applications = parsedApplications.size,
            events = parsedEvents.size,
            derivedDeadlines = seed.candidatures.count { it.dateCloture != null },
        )
    }

    private fun idFor(key: String): String =
        UUID.nameUUIDFromBytes(key.toByteArray()).toString()

    companion object {
        const val ASSET_NAME = "candidatures.json"
    }
}

data class ImportReport(
    val applications: Int,
    val events: Int,
    val derivedDeadlines: Int,
)
