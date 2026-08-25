package tg.goddivor.jobcalender.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class JobCalenderDatabaseTest {

    private lateinit var database: JobCalenderDatabase
    private lateinit var applicationDao: ApplicationDao
    private lateinit var eventDao: EventDao

    private val now = Instant.parse("2026-08-25T06:00:00Z")

    // The real Institut Meridien application: a reference, a named contact, and an interview whose link had
    // still not arrived the day before.
    private val ccdg = ApplicationEntity(
        id = "app-ccdg",
        employer = "Institut Meridien",
        position = "Assistant en Informatique",
        reference = "IM/2026/SI/07",
        channel = Channel.EMAIL,
        status = Status.INTERVIEW,
        sentAt = LocalDate.of(2026, 6, 17),
        folder = "MERIDIEN_AssistantInfo",
        contactName = "A. Kodjo",
        contactEmail = "contact@exemple.invalid",
        updatedAt = now,
    )

    private val acknowledgement = EventEntity(
        id = "ev-ccdg-ack",
        applicationId = ccdg.id,
        type = EventType.ACKNOWLEDGEMENT,
        date = LocalDate.of(2026, 6, 24),
        updatedAt = now,
    )

    private val interview = EventEntity(
        id = "ev-ccdg-interview",
        applicationId = ccdg.id,
        type = EventType.INTERVIEW,
        date = LocalDate.of(2026, 8, 27),
        time = LocalTime.of(11, 30),
        mode = EventMode.VIDEO,
        link = null,
        outcome = EventOutcome.PENDING,
        note = "lien a recevoir",
        updatedAt = now,
    )

    @Before
    fun createDatabase() {
        // getApplicationContext() is itself generic, so the context type has to be spelled out
        // or Kotlin cannot infer the builder's type parameter.
        database = Room.inMemoryDatabaseBuilder<JobCalenderDatabase>(
            ApplicationProvider.getApplicationContext<Context>(),
        )
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        applicationDao = database.applicationDao()
        eventDao = database.eventDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun writesThenReadsBackAnApplicationWithTwoEvents() = runTest {
        applicationDao.upsert(ccdg)
        eventDao.upsertAll(listOf(acknowledgement, interview))

        val stored = applicationDao.withEvents(ccdg.id).first()

        assertNotNull(stored)
        assertEquals("Institut Meridien", stored!!.application.employer)
        assertEquals(Status.INTERVIEW, stored.application.status)
        assertEquals(LocalDate.of(2026, 6, 17), stored.application.sentAt)
        assertEquals(2, stored.events.size)
    }

    @Test
    fun keepsNullsAsNullsRatherThanEmptyStrings() = runTest {
        applicationDao.upsert(ccdg)
        eventDao.upsert(interview)

        val stored = eventDao.forApplication(ccdg.id).first().single()

        assertNull("a link that never arrived must stay null", stored.link)
        assertNull(stored.location)
        assertEquals(LocalTime.of(11, 30), stored.time)
        assertEquals(EventMode.VIDEO, stored.mode)
        assertNull("no closing date was communicated", ccdg.closingDate)
    }

    @Test
    fun ordersUpcomingEventsByDateThenTimeWithUntimedLast() = runTest {
        applicationDao.upsert(ccdg)
        val untimedSameDay = interview.copy(id = "ev-untimed", time = null)
        eventDao.upsertAll(listOf(interview, untimedSameDay))

        val upcoming = eventDao.upcoming(LocalDate.of(2026, 8, 25)).first()

        assertEquals(2, upcoming.size)
        assertEquals("ev-ccdg-interview", upcoming.first().event.id)
        assertEquals("ev-untimed", upcoming.last().event.id)
    }

    @Test
    fun deletingAnApplicationCascadesToItsEvents() = runTest {
        applicationDao.upsert(ccdg)
        eventDao.upsertAll(listOf(acknowledgement, interview))
        assertEquals(2, eventDao.count())

        applicationDao.delete(ccdg)

        assertEquals(0, applicationDao.count())
        assertEquals("events must not outlive their application", 0, eventDao.count())
    }

    @Test
    fun findsAnApplicationByItsFolderName() = runTest {
        applicationDao.upsert(ccdg)

        val found = applicationDao.findByFolder("MERIDIEN_AssistantInfo")

        assertNotNull("folder is the join key the jobing MCP will use", found)
        assertEquals(ccdg.id, found!!.id)
    }

    @Test
    fun countsApplicationsByStatus() = runTest {
        applicationDao.upsertAll(
            listOf(
                ccdg,
                ccdg.copy(id = "app-palma", employer = "Studio Lagune", folder = "LAGUNE_Frontend"),
                ccdg.copy(id = "app-atd", employer = "Atelier Numerique", status = Status.TEST, folder = "ATELIER_Fullstack"),
            ),
        )

        val counts = applicationDao.countByStatus().first().associate { it.status to it.count }

        assertEquals(2, counts[Status.INTERVIEW])
        assertEquals(1, counts[Status.TEST])
        assertTrue("a status with no row must simply be absent", counts[Status.OFFER] == null)
    }
}
