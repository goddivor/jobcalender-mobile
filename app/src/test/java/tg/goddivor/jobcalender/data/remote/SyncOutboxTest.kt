package tg.goddivor.jobcalender.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import tg.goddivor.jobcalender.data.local.PendingWriteDao
import tg.goddivor.jobcalender.data.local.PendingWriteEntity
import tg.goddivor.jobcalender.domain.model.Event
import tg.goddivor.jobcalender.domain.model.EventType
import java.time.Instant
import java.time.LocalDate

private class FakeDao : PendingWriteDao {
    val rows = mutableListOf<PendingWriteEntity>()
    private var nextId = 1L
    override suspend fun all() = rows.sortedBy { it.id }
    override suspend fun count() = rows.size
    override suspend fun insert(write: PendingWriteEntity) {
        rows += write.copy(id = nextId++)
    }
    override suspend fun delete(id: Long) {
        rows.removeAll { it.id == id }
    }
    override suspend fun recordAttempt(id: Long) {
        val index = rows.indexOfFirst { it.id == id }
        rows[index] = rows[index].copy(attempts = rows[index].attempts + 1)
    }
}

private class RecordingApi(private val code: Int = 200) : SyncApi by NotCalled {
    data class Call(val method: String, val url: String, val body: String?)

    val calls = mutableListOf<Call>()

    private fun answer(method: String, url: String, body: RequestBody?): Response<ResponseBody> {
        calls += Call(method, url, body?.let { Buffer().also { b -> it.writeTo(b) }.readUtf8() })
        val payload = "{}".toResponseBody("application/json".toMediaType())
        return if (code in 200..299) {
            Response.success(payload)
        } else {
            Response.error(code, payload)
        }
    }

    override suspend fun putApplication(url: String, bearer: String, writer: String, body: RequestBody) =
        answer("PUT", url, body)

    override suspend fun patchApplication(url: String, bearer: String, writer: String, body: RequestBody) =
        answer("PATCH", url, body)

    override suspend fun postEvent(url: String, bearer: String, writer: String, body: RequestBody) =
        answer("POST", url, body)

    override suspend fun deleteDocument(url: String, bearer: String, writer: String) =
        answer("DELETE", url, null)
}

private object NotCalled : SyncApi {
    private fun no(): Nothing = error("not part of this test")
    override suspend fun config(url: String, key: String) = no()
    override suspend fun pull(url: String, bearer: String) = no()
    override suspend fun status(url: String, bearer: String) = no()
    override suspend fun putApplication(url: String, bearer: String, writer: String, body: RequestBody) = no()
    override suspend fun patchApplication(url: String, bearer: String, writer: String, body: RequestBody) = no()
    override suspend fun postEvent(url: String, bearer: String, writer: String, body: RequestBody) = no()
    override suspend fun deleteDocument(url: String, bearer: String, writer: String) = no()
}

class SyncOutboxTest {

    private val configured = SyncState(
        apiUrl = "https://example.test",
        token = "t",
    )

    /** Nothing is sent on its own here: every test drains when it chooses to. */
    private fun outbox(dao: PendingWriteDao, api: SyncApi) = SyncOutbox(dao, api) { null }

    @Test
    fun `a changed field is sent as the PATCH body, not swallowed`() = runTest {
        val dao = FakeDao()
        val api = RecordingApi()
        val box = outbox(dao, api)

        box.queueApplicationChanged("a1", JsonObject(mapOf("note" to JsonPrimitive("relance"))))
        box.drain(configured)

        assertEquals(1, api.calls.size)
        assertEquals("PATCH", api.calls[0].method)
        assertEquals("https://example.test/api/applications/a1", api.calls[0].url)
        assertTrue(api.calls[0].body!!.contains("relance"))
        assertEquals(0, dao.count())
    }

    @Test
    fun `an event is posted under its application`() = runTest {
        val dao = FakeDao()
        val api = RecordingApi()
        val box = outbox(dao, api)

        box.queueEventSaved(
            Event(
                id = "e1",
                applicationId = "a1",
                type = EventType.INTERVIEW,
                date = LocalDate.parse("2026-08-27"),
                updatedAt = Instant.parse("2026-08-26T09:00:00Z"),
            ),
        )
        box.drain(configured)

        assertEquals("POST", api.calls[0].method)
        assertEquals("https://example.test/api/applications/a1/events", api.calls[0].url)
    }

    @Test
    fun `a refused write is dropped, a broken connection keeps it`() = runTest {
        val refused = FakeDao()
        outbox(refused, RecordingApi(code = 400)).also {
            it.queueApplicationDeleted("a1")
            it.drain(configured)
        }
        assertEquals(0, refused.count())

        val unreachable = FakeDao()
        outbox(unreachable, RecordingApi(code = 503)).also {
            it.queueApplicationDeleted("a1")
            it.drain(configured)
        }
        assertEquals(1, unreachable.count())
    }
}
