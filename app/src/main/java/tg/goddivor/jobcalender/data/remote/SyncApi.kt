package tg.goddivor.jobcalender.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface SyncApi {

    /** The only route the compiled-in key opens. Everything else needs the bearer token it hands back. */
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

    @POST
    suspend fun push(
        @Url url: String,
        @Header("Authorization") bearer: String,
        @Body body: PushRequest,
    ): PushResponse

    @GET
    suspend fun status(
        @Url url: String,
        @Header("Authorization") bearer: String,
    ): StatusResponse
}
