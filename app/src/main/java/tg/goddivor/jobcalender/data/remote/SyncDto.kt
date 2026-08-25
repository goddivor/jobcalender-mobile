package tg.goddivor.jobcalender.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shape, identical to what the server stores and to what the jobing MCP will write.
 * Enums travel as their Kotlin names so no layer translates a schema.
 */
@Serializable
data class ApplicationDto(
    @SerialName("_id") val id: String,
    val employer: String,
    val position: String,
    val reference: String? = null,
    val channel: String,
    val status: String,
    val sentAt: String? = null,
    val closingDate: String? = null,
    val folder: String? = null,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val note: String? = null,
    val updatedAt: String,
    val events: List<EventDto> = emptyList(),
)

@Serializable
data class EventDto(
    val id: String,
    val type: String,
    val date: String,
    val time: String? = null,
    val durationMinutes: Int? = null,
    val mode: String? = null,
    val location: String? = null,
    val link: String? = null,
    val outcome: String? = null,
    val note: String? = null,
    val updatedAt: String,
)

@Serializable
data class ConfigResponse(
    val ok: Boolean = true,
    @SerialName("api_url") val apiUrl: String,
    val token: String,
)

@Serializable
data class SnapshotMeta(
    @SerialName("pushed_at") val pushedAt: String? = null,
    val device: String? = null,
    val counts: Counts? = null,
)

@Serializable
data class Counts(
    val applications: Int = 0,
    val events: Int = 0,
)

@Serializable
data class PullResponse(
    val ok: Boolean = true,
    val meta: SnapshotMeta? = null,
    val applications: List<ApplicationDto> = emptyList(),
)

@Serializable
data class PushRequest(
    val device: String,
    val applications: List<ApplicationDto>,
)

@Serializable
data class PushResponse(
    val ok: Boolean = true,
    val meta: SnapshotMeta? = null,
)

@Serializable
data class StatusResponse(
    val ok: Boolean = true,
    val meta: SnapshotMeta? = null,
)
