package tg.goddivor.jobcalender.data.remote

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.Status
import java.time.Instant
import java.time.LocalDate

class ApplicationDiffTest {

    private val base = Application(
        id = "a1",
        employer = "CCDG (CEDEAO)",
        position = "Assistant en Informatique",
        channel = Channel.EMAIL,
        status = Status.SENT,
        sentAt = LocalDate.parse("2026-08-01"),
        note = "premier contact",
        updatedAt = Instant.parse("2026-08-01T10:00:00Z"),
    )

    @Test
    fun `an untouched application produces nothing to send`() {
        assertTrue(changedFields(base, base).isEmpty())
    }

    @Test
    fun `only the changed field travels, with the new timestamp`() {
        val after = base.copy(
            status = Status.INTERVIEW,
            updatedAt = Instant.parse("2026-08-26T09:00:00Z"),
        )
        val changes = changedFields(base, after)

        assertEquals(setOf("status", "updatedAt"), changes.keys)
        assertEquals("INTERVIEW", changes["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `a field the user cleared travels as an explicit null`() {
        val after = base.copy(note = null, updatedAt = Instant.parse("2026-08-26T09:00:00Z"))
        val changes = changedFields(base, after)

        assertEquals(setOf("note", "updatedAt"), changes.keys)
        assertEquals(JsonNull, changes["note"])
    }

    @Test
    fun `a field filled in later travels alone, leaving the rest untouched`() {
        val after = base.copy(
            closingDate = LocalDate.parse("2026-09-15"),
            updatedAt = Instant.parse("2026-08-26T09:00:00Z"),
        )
        val changes = changedFields(base, after)

        assertEquals(setOf("closingDate", "updatedAt"), changes.keys)
        assertEquals("2026-09-15", changes["closingDate"]?.jsonPrimitive?.content)
    }
}
