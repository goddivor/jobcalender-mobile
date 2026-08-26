package tg.goddivor.jobcalender.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Url

/**
 * The app reads the base and corrects one document at a time. `/api/push`, which replaces the whole
 * collection, is deliberately absent: the jobing MCP is the first writer, and a snapshot sent from
 * here would erase whatever it wrote since the last pull. A restore goes through that route from a
 * tool on the machine, with its explicit confirmation header.
 */
interface SyncApi {

    /** The only route the configuration key opens. Everything else needs the bearer token it returns. */
    @GET
    suspend fun config(
        @Url url: String,
        @Header("X-Config-Key") key: String,
    ): ConfigResponse

    @GET
    suspend fun pull(
        @Url url: String,
        @Header("Authorization") bearer: String,
    ): PullResponse

    @GET
    suspend fun status(
        @Url url: String,
        @Header("Authorization") bearer: String,
    ): StatusResponse

    // The four write routes carry a raw body: what to send was decided when the edit was queued,
    // and re-encoding it here would be a second chance to disagree with the server about a shape.
    // Response rather than a decoded type, so a refusal can be told from a broken connection.

    @PUT
    suspend fun putApplication(
        @Url url: String,
        @Header("Authorization") bearer: String,
        @Header("X-Writer") writer: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @PATCH
    suspend fun patchApplication(
        @Url url: String,
        @Header("Authorization") bearer: String,
        @Header("X-Writer") writer: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @POST
    suspend fun postEvent(
        @Url url: String,
        @Header("Authorization") bearer: String,
        @Header("X-Writer") writer: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>

    @DELETE
    suspend fun deleteDocument(
        @Url url: String,
        @Header("Authorization") bearer: String,
        @Header("X-Writer") writer: String,
    ): Response<ResponseBody>
}
