package tg.goddivor.jobcalender.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import tg.goddivor.jobcalender.domain.model.EventMode

class MeetingBrandTest {

    @Test
    fun `a known host decides, whatever the mode claims`() {
        assertEquals(
            MeetingBrand.TEAMS,
            meetingBrand("https://teams.microsoft.com/meet/351825696155163", EventMode.VIDEO),
        )
        assertEquals(
            MeetingBrand.GOOGLE_MEET,
            meetingBrand("https://meet.google.com/abc-defg-hij", EventMode.VIDEO),
        )
        assertEquals(MeetingBrand.ZOOM, meetingBrand("https://us02web.zoom.us/j/123", null))
    }

    @Test
    fun `the mode answers when the host is a booking service`() {
        // The real PALMA invitation: a booking host for a meeting held on Google Meet.
        assertEquals(
            MeetingBrand.GOOGLE_MEET,
            meetingBrand(
                "https://book.youcanbook.me/google-meet/DEFD-VDDG-FEXS",
                EventMode.GOOGLE_MEET,
            ),
        )
    }

    @Test
    fun `a product named in the path proves nothing`() {
        assertEquals(MeetingBrand.NONE, meetingBrand("https://example.test/go?to=zoom.us", null))
        assertEquals(MeetingBrand.NONE, meetingBrand("https://intranet.test/teams/meeting", null))
    }

    @Test
    fun `no link and no mode leaves it undecided`() {
        assertEquals(MeetingBrand.NONE, meetingBrand(null, null))
        assertEquals(MeetingBrand.NONE, meetingBrand("", EventMode.VIDEO))
        assertEquals(MeetingBrand.NONE, meetingBrand(null, EventMode.ONSITE))
    }

    @Test
    fun `the host is read whatever surrounds it`() {
        assertEquals(MeetingBrand.TEAMS, meetingBrand("HTTPS://Teams.Microsoft.com/l/x", null))
        assertEquals(MeetingBrand.TEAMS, meetingBrand("  https://www.teams.live.com/x  ", null))
        assertEquals(MeetingBrand.GOOGLE_MEET, meetingBrand("https://meet.google.com:443/x", null))
    }
}
