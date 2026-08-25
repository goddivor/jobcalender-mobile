package tg.goddivor.jobcalender.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class ScheduleConflictsTest {

    private val day = LocalDate.of(2026, 8, 24)
    private val otherDay = LocalDate.of(2026, 8, 25)
    private val stamp = Instant.parse("2026-08-25T06:00:00Z")

    private fun event(
        id: String,
        type: EventType,
        time: String?,
        minutes: Int? = null,
        date: LocalDate = day,
    ) = Event(
        id = id,
        applicationId = "any",
        type = type,
        date = date,
        time = time?.let(LocalTime::parse),
        durationMinutes = minutes,
        updatedAt = stamp,
    )

    @Test
    fun `the 24 August clash is detected`() {
        // The meeting that was actually missed, against the test that caused it.
        val meeting = event("meeting", EventType.MEETING, "09:30")
        val test = event("test", EventType.TEST, "10:00")

        val report = detectConflicts(listOf(meeting, test))

        assertTrue(report.involves(meeting))
        assertTrue(report.involves(test))
        assertTrue(report.hasConflictOn(day))
    }

    @Test
    fun `events that merely touch do not overlap`() {
        val first = event("first", EventType.MEETING, "09:00", minutes = 60)
        val second = event("second", EventType.TEST, "10:00")

        val report = detectConflicts(listOf(first, second))

        assertTrue("09:00 to 10:00 ends exactly where 10:00 starts", report.eventIds.isEmpty())
    }

    @Test
    fun `a declared duration is preferred over the default hour`() {
        val short = event("short", EventType.INTERVIEW, "09:30", minutes = 20)
        val next = event("next", EventType.TEST, "10:00")

        assertTrue(detectConflicts(listOf(short, next)).eventIds.isEmpty())
        assertTrue(detectConflicts(listOf(short.copy(durationMinutes = null), next)).involves(short))
    }

    @Test
    fun `an event without an hour never conflicts`() {
        val untimed = event("untimed", EventType.TEST, null)
        val timed = event("timed", EventType.INTERVIEW, "10:00")

        val report = detectConflicts(listOf(untimed, timed))

        assertFalse("guessing a time would invent a clash", report.involves(untimed))
        assertTrue(report.eventIds.isEmpty())
    }

    @Test
    fun `events on different days never conflict`() {
        val first = event("first", EventType.INTERVIEW, "10:00")
        val second = event("second", EventType.TEST, "10:00", date = otherDay)

        assertTrue(detectConflicts(listOf(first, second)).eventIds.isEmpty())
    }

    @Test
    fun `three overlapping events all report as conflicting`() {
        val meeting = event("meeting", EventType.MEETING, "09:30")
        val test = event("test", EventType.TEST, "10:00")
        val interview = event("interview", EventType.INTERVIEW, "10:00")

        val report = detectConflicts(listOf(meeting, test, interview))

        assertEquals(setOf("meeting", "test", "interview"), report.eventIds)
        assertEquals(setOf(day), report.days)
    }

    @Test
    fun `an empty list produces an empty report`() {
        val report = detectConflicts(emptyList())
        assertTrue(report.eventIds.isEmpty())
        assertTrue(report.days.isEmpty())
    }
}
