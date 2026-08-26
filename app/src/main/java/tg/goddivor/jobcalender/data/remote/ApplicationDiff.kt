package tg.goddivor.jobcalender.data.remote

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tg.goddivor.jobcalender.domain.model.Application

/**
 * The fields a manual edit actually touched, ready to be sent as a PATCH body.
 *
 * Sending the whole document instead would undo anything the jobing MCP wrote to the same
 * application since the last pull. A field the user cleared travels as an explicit null, which the
 * server stores; a field left alone is simply absent.
 */
fun changedFields(before: Application, after: Application): JsonObject {
    val changes = mutableMapOf<String, JsonPrimitive?>()

    fun compare(name: String, old: Any?, new: Any?) {
        if (old != new) changes[name] = new?.let { JsonPrimitive(it.toString()) }
    }

    compare("employer", before.employer, after.employer)
    compare("position", before.position, after.position)
    compare("reference", before.reference, after.reference)
    compare("channel", before.channel, after.channel)
    compare("status", before.status, after.status)
    compare("sentAt", before.sentAt, after.sentAt)
    compare("closingDate", before.closingDate, after.closingDate)
    compare("folder", before.folder, after.folder)
    compare("contactName", before.contactName, after.contactName)
    compare("contactEmail", before.contactEmail, after.contactEmail)
    compare("contactPhone", before.contactPhone, after.contactPhone)
    compare("note", before.note, after.note)

    if (changes.isEmpty()) return JsonObject(emptyMap())
    changes["updatedAt"] = JsonPrimitive(after.updatedAt.toString())
    return JsonObject(changes.mapValues { (_, value) -> value ?: JsonNull })
}
