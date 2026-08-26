package tg.goddivor.jobcalender.data.remote

import android.os.Build
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncResult {
    data class Pushed(val applications: Int, val events: Int) : SyncResult
    data class Pulled(val applications: Int, val events: Int) : SyncResult
    data object AlreadyUpToDate : SyncResult
    data object NotConfigured : SyncResult
    data class Failed(val reason: String) : SyncResult
}

/** Why fetching the configuration ended the way it did, so the form can say which one happened. */
sealed interface ConfigResult {
    data object Success : ConfigResult
    data object InvalidKey : ConfigResult
    data object Offline : ConfigResult
    data class ServerError(val code: Int) : ConfigResult
}

/**
 * Snapshot sync, one direction at a time. Whichever side carries the newest `updatedAt` wins and
 * overwrites the other outright: no merge, no diff, no conflict resolution. One user, one device, a
 * tiny volume, and that is the whole point.
 *
 * A failure is never fatal. The database is local and authoritative; the app stays fully usable
 * with no network at all.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val api: SyncApi,
    private val settings: SyncSettings,
    private val applications: ApplicationRepository,
    private val events: EventRepository,
) {

    suspend fun sync(): SyncResult = runCatching { attempt() }
        .getOrElse { error -> SyncResult.Failed(error.readable()) }

    private suspend fun attempt(): SyncResult {
        val configured = ensureConfigured() ?: return SyncResult.NotConfigured
        val bearer = "Bearer ${configured.token}"

        val local = applications.allWithEvents().first()
        val localNewest = local.flatMap { entry ->
            listOf(entry.application.updatedAt) + entry.events.map { it.updatedAt }
        }.maxOrNull()

        val remote = runCatching { api.pull("${configured.apiUrl}/api/pull", bearer) }
            .getOrElse { error ->
                // 404 simply means the cloud has never been written to, which is a normal first run.
                if ((error as? HttpException)?.code() == NOT_FOUND) null else throw error
            }

        val remoteNewest = remote?.applications?.flatMap { dto ->
            listOf(Instant.parse(dto.updatedAt)) + dto.events.map { Instant.parse(it.updatedAt) }
        }?.maxOrNull()

        return when {
            remoteNewest == null && localNewest == null -> SyncResult.AlreadyUpToDate
            remoteNewest == null -> push(configured, bearer, local)
            localNewest == null -> pull(remote)
            localNewest > remoteNewest -> push(configured, bearer, local)
            remoteNewest > localNewest -> pull(remote)
            else -> {
                settings.markSynced(Instant.now())
                SyncResult.AlreadyUpToDate
            }
        }
    }

    private suspend fun push(
        configured: SyncState,
        bearer: String,
        local: List<tg.goddivor.jobcalender.domain.model.ApplicationWithEvents>,
    ): SyncResult {
        val payload = PushRequest(device = Build.MODEL ?: "android", applications = local.map { it.toDto() })
        val response = api.push("${configured.apiUrl}/api/push", bearer, payload)
        settings.markSynced(Instant.now())
        return SyncResult.Pushed(
            applications = response.meta?.counts?.applications ?: payload.applications.size,
            events = response.meta?.counts?.events ?: payload.applications.sumOf { it.events.size },
        )
    }

    private suspend fun pull(remote: PullResponse): SyncResult {
        // Replace, not merge: the local copy becomes exactly what the cloud holds.
        applications.allWithEvents().first().forEach { applications.delete(it.application) }
        applications.upsertAll(remote.applications.map { it.toApplication() })
        events.upsertAll(
            remote.applications.flatMap { dto -> dto.events.map { it.toEvent(dto.id) } },
        )
        settings.markSynced(Instant.now())
        return SyncResult.Pulled(
            applications = remote.applications.size,
            events = remote.applications.sumOf { it.events.size },
        )
    }

    /**
     * Exchanges a typed key for the address and the bearer token. The key is not kept: it opens
     * `/api/config` once and nothing else, and refusing to store it is what keeps a stolen device
     * or a decompiled APK from re-issuing a token.
     */
    suspend fun configure(serverUrl: String, key: String): ConfigResult {
        val url = serverUrl.trim().trimEnd('/')
        settings.setServerUrl(url)
        return runCatching { api.config("$url/api/config", key.trim()) }
            .fold(
                onSuccess = { config ->
                    settings.store(config.apiUrl, config.token)
                    ConfigResult.Success
                },
                onFailure = { error ->
                    when {
                        error !is HttpException -> ConfigResult.Offline
                        error.code() == UNAUTHORIZED || error.code() == FORBIDDEN ->
                            ConfigResult.InvalidKey
                        else -> ConfigResult.ServerError(error.code())
                    }
                },
            )
    }

    /** Nothing is fetched on the app's own initiative any more: the user configures, or there is no sync. */
    private suspend fun ensureConfigured(): SyncState? =
        settings.state.first().takeIf { it.isConfigured }

    private fun Throwable.readable(): String = when (this) {
        is HttpException -> "HTTP ${code()}"
        else -> this::class.simpleName ?: "erreur"
    }

    private companion object {
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
    }
}
