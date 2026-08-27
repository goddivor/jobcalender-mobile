package tg.goddivor.jobcalender.domain.usecase

import tg.goddivor.jobcalender.domain.model.EventMode

/** Which product a remote appointment will open, when it can be told. */
enum class MeetingBrand { TEAMS, GOOGLE_MEET, ZOOM, NONE }

private val HOSTS = mapOf(
    "teams.microsoft.com" to MeetingBrand.TEAMS,
    "teams.live.com" to MeetingBrand.TEAMS,
    "meet.google.com" to MeetingBrand.GOOGLE_MEET,
    "zoom.us" to MeetingBrand.ZOOM,
)

/**
 * The link decides when its host is one we know, because it is the link that will actually open. The
 * mode answers otherwise, and it often can: a real invitation reads
 * `book.youcanbook.me/google-meet/…`, a booking host for a meeting held on Google Meet.
 *
 * Matching the host and not the whole address on purpose: `…/redirect?to=zoom.us` is not a Zoom
 * meeting, and a path that merely mentions a product proves nothing about where it will land.
 */
fun meetingBrand(link: String?, mode: EventMode?): MeetingBrand {
    fromHost(link)?.let { return it }
    return when (mode) {
        EventMode.TEAMS -> MeetingBrand.TEAMS
        EventMode.GOOGLE_MEET -> MeetingBrand.GOOGLE_MEET
        else -> MeetingBrand.NONE
    }
}

private fun fromHost(link: String?): MeetingBrand? {
    val host = link?.trim()?.lowercase()
        ?.substringAfter("://", "")
        ?.substringBefore('/')
        ?.substringBefore('?')
        ?.substringAfter('@')
        ?.substringBefore(':')
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return HOSTS.entries.firstOrNull { (known, _) -> host == known || host.endsWith(".$known") }?.value
}
