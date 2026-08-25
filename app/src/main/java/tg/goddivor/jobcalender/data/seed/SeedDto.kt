package tg.goddivor.jobcalender.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors candidatures.json exactly, French field names included. The file is never edited to suit
 * the code: the code bends to the file.
 *
 * The asset shipped here is a demonstration set, because this repository is public. The owner's real
 * applications live in the private context repository and on the device, and reach a fresh install
 * through the first sync, which runs before seeding for exactly that reason.
 */
@Serializable
data class SeedFile(
    @SerialName("exported_at") val exportedAt: String,
    val candidatures: List<SeedApplication>,
)

@Serializable
data class SeedApplication(
    val employeur: String,
    val poste: String,
    val reference: String? = null,
    val canal: String,
    @SerialName("envoyee_le") val envoyeeLe: String? = null,
    val statut: String,
    val dossier: String,
    @SerialName("date_cloture") val dateCloture: String? = null,
    val contact: SeedContact? = null,
    val note: String? = null,
    val evenements: List<SeedEvent> = emptyList(),
)

@Serializable
data class SeedContact(
    val nom: String? = null,
    val email: String? = null,
    val whatsapp: String? = null,
)

@Serializable
data class SeedEvent(
    val type: String,
    val date: String,
    val heure: String? = null,
    @SerialName("duree_min") val dureeMin: Int? = null,
    val fuseau: String? = null,
    val mode: String? = null,
    val lieu: String? = null,
    val lien: String? = null,
    val reference: String? = null,
    val statut: String? = null,
    val note: String? = null,
)
