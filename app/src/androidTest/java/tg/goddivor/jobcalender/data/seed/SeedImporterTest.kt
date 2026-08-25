package tg.goddivor.jobcalender.data.seed

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tg.goddivor.jobcalender.data.local.JobCalenderDatabase
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class SeedImporterTest {

    private lateinit var database: JobCalenderDatabase
    private lateinit var importer: SeedImporter
    private lateinit var applications: ApplicationRepository
    private lateinit var events: EventRepository

    @Before
    fun setUp() {
        // The asset lives in the app under test, not in the test APK.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<JobCalenderDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext as Context,
        )
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        applications = ApplicationRepository(database.applicationDao())
        events = EventRepository(database.eventDao())
        importer = SeedImporter(context, applications, events)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importsTheTwentyFiveRealApplications() = runTest {
        val report = importer.import()

        assertEquals(25, report.applications)
        assertEquals(25, applications.count())
        assertEquals("two closing dates become deadline events", 2, report.derivedDeadlines)
        assertEquals("25 events in the file plus 2 derived deadlines", 27, report.events)
        assertEquals(27, events.count())
    }

    @Test
    fun runningTwiceDuplicatesNothing() = runTest {
        importer.import()
        importer.import()

        assertEquals(25, applications.count())
        assertEquals(27, events.count())
    }

    @Test
    fun importIfEmptySkipsAnAlreadyPopulatedDatabase() = runTest {
        assertNotNull(importer.importIfEmpty())

        assertNull("a populated database must not be re-imported", importer.importIfEmpty())
        assertEquals(25, applications.count())
    }

    @Test
    fun keepsTheCcdgInterviewExactlyAsExported() = runTest {
        importer.import()

        val ccdg = applications.findByFolder("MERIDIEN_AssistantInfo")
        assertNotNull(ccdg)
        assertEquals("Institut Meridien", ccdg!!.employer)
        assertEquals(Status.INTERVIEW, ccdg.status)
        assertEquals("IM/2026/SI/07", ccdg.reference)
        assertEquals("contact@exemple.invalid", ccdg.contactEmail)

        val interview = events.forApplication(ccdg.id).first()
            .single { it.type == EventType.INTERVIEW }
        assertEquals(LocalDate.of(2026, 8, 27), interview.date)
        assertEquals(LocalTime.of(11, 30), interview.time)
        assertEquals(EventMode.VIDEO, interview.mode)
        assertNull("the link had still not arrived", interview.link)
    }

    @Test
    fun derivesADeadlineFromTheUndpClosingDate() = runTest {
        importer.import()

        val undp = applications.findByFolder("ALIZES_Stage")
        assertNotNull(undp)
        assertEquals(LocalDate.of(2026, 10, 1), undp!!.closingDate)

        val deadline = events.forApplication(undp.id).first()
            .single { it.type == EventType.DEADLINE }
        assertEquals(LocalDate.of(2026, 10, 1), deadline.date)
        assertNull("a deadline has no hour", deadline.time)
    }

    @Test
    fun keepsTheWhatsAppOnlyContactOfAgenceTogoDigital() = runTest {
        importer.import()

        val atd = applications.findByFolder("ATELIER_Fullstack")
        assertNotNull(atd)
        assertEquals("+22890000000", atd!!.contactPhone)
        assertNull("that contact gave no email", atd.contactEmail)
    }

    @Test
    fun refusesAnUnknownFrenchValueLoudly() {
        assertThrows(UnknownSeedValueException::class.java) { statusOf("en_cours") }
        assertThrows(UnknownSeedValueException::class.java) { channelOf("pigeon") }
        assertThrows(UnknownSeedValueException::class.java) { eventTypeOf("cafe") }
    }
}
