package tg.goddivor.jobcalender.data.remote

import android.os.Build
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import tg.goddivor.jobcalender.data.local.PendingWriteDao
import tg.goddivor.jobcalender.data.local.PendingWriteEntity
import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.Event
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What a queued write asks the server to do. Stored as text, so a new verb never breaks an old row. */
object Verb {
    const val PUT_APPLICATION = "PUT_APPLICATION"
    const val PATCH_APPLICATION = "PATCH_APPLICATION"
    const val DELETE_APPLICATION = "DELETE_APPLICATION"
    const val POST_EVENT = "POST_EVENT"
    const val DELETE_EVENT = "DELETE_EVENT"
}

data class DrainReport(val sent: Int, val dropped: Int, val remaining: Int)

/**
 * Local edits waiting to reach the server, one document each.
 *
 * The app is this base's reader: the jobing MCP writes it first. So an edit made here never travels
 * as a snapshot, only as the single document it touched, and a correction to a field the MCP filled
 * in the meantime leaves that field alone. Queuing rather than sending straight away is what keeps
 * the app usable with no network: the row waits, and the next sync carries it.
 */
@Singleton
class SyncOutbox @Inject constructor(
    private val dao: PendingWriteDao,
    private val api: SyncApi,
) {
    private val json = Json { encodeDefaults = false; explicitNulls = false }

    suspend fun pending(): Int = dao.count()

    suspend fun queueApplicationCreated(application: Application) = queue(
        verb = Verb.PUT_APPLICATION,
        applicationId = application.id,
        body = json.encodeToString(application.toDto(emptyList())),
    )

    /** Only the fields that actually changed: the rest of the document belongs to whoever wrote it. */
    suspend fun queueApplicationChanged(id: String, changes: JsonObject) {
        if (changes.isEmpty()) return
        queue(Verb.PATCH_APPLICATION, id, json.encodeToString(JsonObject.serializer(), changes))
    }

    suspend fun queueApplicationDeleted(id: String) = queue(Verb.DELETE_APPLICATION, id)

    suspend fun queueEventSaved(event: Event) = queue(
        verb = Verb.POST_EVENT,
        applicationId = event.applicationId,
        eventId = event.id,
        body = json.encodeToString(event.toDto()),
    )

    suspend fun queueEventDeleted(event: Event) =
        queue(Verb.DELETE_EVENT, event.applicationId, event.id)

    private suspend fun queue(
        verb: String,
        applicationId: String,
        eventId: String? = null,
        body: String? = null,
    ) = dao.insert(
        PendingWriteEntity(
            verb = verb,
            applicationId = applicationId,
            eventId = eventId,
            body = body,
            queuedAt = Instant.now(),
        ),
    )

    /**
     * Sends what is waiting, oldest first, and stops at the first entry the network refused: a later
     * edit must never overtake the one it corrects. A refusal the server will repeat, such as a
     * malformed body or a document that no longer exists, is dropped rather than retried for ever.
     */
    suspend fun drain(state: SyncState): DrainReport {
        val bearer = "Bearer ${state.token}"
        val base = state.apiUrl?.trimEnd('/') ?: return DrainReport(0, 0, dao.count())
        var sent = 0
        var dropped = 0

        for (write in dao.all()) {
            val outcome = runCatching { send(base, bearer, write) }
                .getOrElse { Outcome.RETRY }
            when (outcome) {
                Outcome.SENT -> {
                    dao.delete(write.id)
                    sent++
                }
                Outcome.DROP -> {
                    dao.delete(write.id)
                    dropped++
                }
                Outcome.RETRY -> {
                    dao.recordAttempt(write.id)
                    if (write.attempts + 1 >= MAX_ATTEMPTS) {
                        dao.delete(write.id)
                        dropped++
                        continue
                    }
                    return DrainReport(sent, dropped, dao.count())
                }
            }
        }
        return DrainReport(sent, dropped, dao.count())
    }

    private enum class Outcome { SENT, DROP, RETRY }

    private suspend fun send(base: String, bearer: String, write: PendingWriteEntity): Outcome {
        val application = "$base/api/applications/${write.applicationId}"
        val body = write.body?.toRequestBody(JSON_MEDIA)
        val response = when (write.verb) {
            Verb.PUT_APPLICATION -> api.putApplication(application, bearer, WRITER, body!!)
            Verb.PATCH_APPLICATION -> api.patchApplication(application, bearer, WRITER, body!!)
            Verb.DELETE_APPLICATION -> api.deleteDocument(application, bearer, WRITER)
            Verb.POST_EVENT -> api.postEvent("$application/events", bearer, WRITER, body!!)
            Verb.DELETE_EVENT ->
                api.deleteDocument("$application/events?eventId=${write.eventId}", bearer, WRITER)
            else -> return Outcome.DROP
        }
        return when {
            response.isSuccessful -> Outcome.SENT
            // The server will answer the same way tomorrow: a bad body, a document already gone, or
            // a full replace this app is not allowed to ask for.
            response.code() in CLIENT_ERRORS && response.code() != UNAUTHORIZED -> Outcome.DROP
            else -> Outcome.RETRY
        }
    }

    private companion object {
        val JSON_MEDIA = "application/json".toMediaType()
        val WRITER: String = Build.MODEL ?: "android"
        const val MAX_ATTEMPTS = 5
        const val UNAUTHORIZED = 401
        val CLIENT_ERRORS = 400..499
    }
}
