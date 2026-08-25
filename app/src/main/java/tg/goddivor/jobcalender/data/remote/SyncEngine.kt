package tg.goddivor.jobcalender.data.remote

import android.os.Build
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import tg.goddivor.jobcalender.BuildConfig
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

    /** Fetches the URL and token once, with the key compiled into the APK, then remembers them. */
    private suspend fun ensureConfigured(): SyncState? {
        val current = settings.state.first()
        if (current.isConfigured) return current

        val url = BuildConfig.SYNC_CONFIG_URL
        val key = BuildConfig.SYNC_CONFIG_KEY
        if (url.isBlank() || key.isBlank()) return null

        val config = api.config("$url/api/config", key)
        settings.store(config.apiUrl, config.token)
        return settings.state.first()
    }

    private fun Throwable.readable(): String = when (this) {
        is HttpException -> "HTTP ${code()}"
        else -> this::class.simpleName ?: "erreur"
    }

    private companion object {
        const val NOT_FOUND = 404
    }
}
