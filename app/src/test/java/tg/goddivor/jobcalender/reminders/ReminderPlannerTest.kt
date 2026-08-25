package tg.goddivor.jobcalender.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tg.goddivor.jobcalender.domain.model.Application
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.EventWithApplication
import tg.goddivor.jobcalender.domain.model.Status
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderPlannerTest {

    private val now = LocalDateTime.of(2026, 8, 25, 8, 0)
    private val stamp = Instant.parse("2026-08-25T06:00:00Z")

    private val application = Application(
        id = "app",
        employer = "Institut Meridien",
        position = "Assistant en Informatique",
        channel = Channel.EMAIL,
        status = Status.INTERVIEW,
        updatedAt = stamp,
    )

    private fun entry(
        id: String,
        type: EventType,
        date: LocalDate,
        time: String? = null,
    ) = EventWithApplication(
        event = Event(
            id = id,
            applicationId = "app",
            type = type,
            date = date,
            time = time?.let(LocalTime::parse),
            updatedAt = stamp,
        ),
        application = application,
    )

    private val all = ReminderPreferences(dayBefore = true, hourBefore = true, closing = true)

    @Test
    fun `an interview gets both an evening-before and an hour-before reminder`() {
        val plan = planReminders(
            listOf(entry("e", EventType.INTERVIEW, LocalDate.of(2026, 8, 27), "11:30")),
            all,
            now,
        )

        assertEquals(2, plan.size)
        val evening = plan.single { it.kind == ReminderKind.DAY_BEFORE }
        assertEquals(LocalDate.of(2026, 8, 26), evening.date)
        assertEquals(LocalTime.of(20, 0), evening.time)

        val hour = plan.single { it.kind == ReminderKind.HOUR_BEFORE }
        assertEquals(LocalDate.of(2026, 8, 27), hour.date)
        assertEquals(LocalTime.of(10, 30), hour.time)
    }

    @Test
    fun `an event with no hour gets no hour-before reminder`() {
        val plan = planReminders(
            listOf(entry("e", EventType.TEST, LocalDate.of(2026, 8, 27))),
            all,
            now,
        )

        assertEquals(1, plan.size)
        assertEquals(ReminderKind.DAY_BEFORE, plan.single().kind)
    }

    @Test
    fun `an hour-before reminder that already passed is dropped`() {
        // The appointment is later today, but its hour-before moment is behind us.
        val plan = planReminders(
            listOf(entry("e", EventType.INTERVIEW, LocalDate.of(2026, 8, 25), "08:30")),
            all,
            now,
        )

        assertTrue("nothing should fire late", plan.isEmpty())
    }

    @Test
    fun `a closing date warns three days ahead, and only when asked`() {
        val events = listOf(entry("e", EventType.DEADLINE, LocalDate.of(2026, 10, 1)))

        val withClosing = planReminders(events, all, now)
        assertEquals(LocalDate.of(2026, 9, 28), withClosing.single().date)

        val without = planReminders(events, all.copy(closing = false), now)
        assertTrue(without.isEmpty())
    }

    @Test
    fun `a milestone is never a reminder`() {
        val plan = planReminders(
            listOf(entry("e", EventType.ACKNOWLEDGEMENT, LocalDate.of(2026, 8, 27))),
            all,
            now,
        )

        assertTrue("an acknowledgement is not somewhere to be", plan.isEmpty())
    }

    @Test
    fun `switching both appointment reminders off plans nothing`() {
        val plan = planReminders(
            listOf(entry("e", EventType.INTERVIEW, LocalDate.of(2026, 8, 27), "11:30")),
            ReminderPreferences(dayBefore = false, hourBefore = false, closing = true),
            now,
        )

        assertTrue(plan.isEmpty())
    }
}
