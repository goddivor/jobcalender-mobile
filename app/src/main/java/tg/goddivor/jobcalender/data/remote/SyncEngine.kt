package tg.goddivor.jobcalender.data.remote

import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncResult {
    data class Pulled(val applications: Int, val events: Int, val sent: Int = 0) : SyncResult
    /** Local edits could not reach the server, so pulling would erase them. Nothing was touched. */
    data class Blocked(val pending: Int) : SyncResult
    /** The server holds nothing while this device holds data: an anomaly, not an instruction. */
    data object RemoteEmpty : SyncResult
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
 * The app reads this base; it does not own it. The jobing MCP on the owner's machine is its first
 * writer, so synchronising means pulling, and an edit made here travels alone through the
 * single-document routes rather than as a snapshot that would erase what the MCP wrote.
 *
 * Order matters: whatever is queued goes out first, and nothing is pulled while something is still
 * waiting, because pulling replaces the local copy and would take the queued edit with it.
 *
 * A failure is never fatal. The database is local and authoritative for display; the app stays
 * fully usable with no network at all.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val api: SyncApi,
    private val settings: SyncSettings,
    private val outbox: SyncOutbox,
    private val applications: ApplicationRepository,
    private val events: EventRepository,
) {

    suspend fun sync(): SyncResult = runCatching { attempt() }
        .getOrElse { error -> SyncResult.Failed(error.readable()) }

    private suspend fun attempt(): SyncResult {
        val configured = ensureConfigured() ?: return SyncResult.NotConfigured
        val bearer = "Bearer ${configured.token}"

        val drained = outbox.drain(configured)
        if (drained.remaining > 0) return SyncResult.Blocked(drained.remaining)

        val remote = runCatching { api.pull("${configured.apiUrl}/api/pull", bearer) }
            .getOrElse { error ->
                // 404 simply means the base has never been written to, which is a normal first run.
                if ((error as? HttpException)?.code() == NOT_FOUND) null else throw error
            }
            ?: return SyncResult.RemoteEmpty

        // A server that answers with nothing, facing a device that holds data, means something went
        // wrong upstream. Propagating that emptiness would destroy the only remaining copy.
        if (remote.applications.isEmpty() && applications.count() > 0) return SyncResult.RemoteEmpty

        return pull(remote, drained.sent)
    }

    private suspend fun pull(remote: PullResponse, sent: Int): SyncResult {
        // Replace, not merge: the local copy becomes exactly what the server holds. Safe here only
        // because the queue was emptied first, so nothing local is lost in the process.
        applications.deleteAllLocally()
        applications.upsertAllLocally(remote.applications.map { it.toApplication() })
        events.upsertAllLocally(
            remote.applications.flatMap { dto -> dto.events.map { it.toEvent(dto.id) } },
        )
        settings.markSynced(Instant.now())
        return SyncResult.Pulled(
            applications = remote.applications.size,
            events = remote.applications.sumOf { it.events.size },
            sent = sent,
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
