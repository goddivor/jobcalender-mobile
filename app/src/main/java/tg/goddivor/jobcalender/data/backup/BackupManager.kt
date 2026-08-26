package tg.goddivor.jobcalender.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tg.goddivor.jobcalender.data.remote.ApplicationDto
import tg.goddivor.jobcalender.data.remote.toApplication
import tg.goddivor.jobcalender.data.remote.toDto
import tg.goddivor.jobcalender.data.remote.toEvent
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.repository.EventRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The file written to disk. Same shape as the wire, so an export can be pushed and back. */
@Serializable
data class BackupSnapshot(
    val version: Int = FORMAT_VERSION,
    val exportedAt: String,
    val applications: List<ApplicationDto>,
)

const val FORMAT_VERSION = 1

sealed interface BackupResult {
    data class Written(val applications: Int, val events: Int) : BackupResult
    data class Restored(val applications: Int, val events: Int) : BackupResult
    data object Unreadable : BackupResult
    data class Failed(val reason: String) : BackupResult
}

/**
 * Export and import go through the document picker, so the app never holds a storage permission and
 * never names a path of its own. Import replaces everything, exactly like a pull.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val applications: ApplicationRepository,
    private val events: EventRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun export(target: Uri): BackupResult = runCatching {
        val local = applications.allWithEvents().first()
        val snapshot = BackupSnapshot(
            exportedAt = Instant.now().toString(),
            applications = local.map { it.toDto() },
        )
        context.contentResolver.openOutputStream(target, "wt")?.use { stream ->
            stream.write(json.encodeToString(snapshot).toByteArray())
        } ?: return BackupResult.Unreadable
        BackupResult.Written(
            applications = snapshot.applications.size,
            events = snapshot.applications.sumOf { it.events.size },
        )
    }.getOrElse { error -> BackupResult.Failed(error.reason()) }

    suspend fun import(source: Uri): BackupResult = runCatching {
        val text = context.contentResolver.openInputStream(source)
            ?.use { it.readBytes().decodeToString() }
            ?: return BackupResult.Unreadable
        val snapshot = runCatching { json.decodeFromString<BackupSnapshot>(text) }.getOrNull()
            ?: return BackupResult.Unreadable

        applications.allWithEvents().first().forEach { applications.delete(it.application) }
        applications.upsertAll(snapshot.applications.map { it.toApplication() })
        events.upsertAll(
            snapshot.applications.flatMap { dto -> dto.events.map { it.toEvent(dto.id) } },
        )
        BackupResult.Restored(
            applications = snapshot.applications.size,
            events = snapshot.applications.sumOf { it.events.size },
        )
    }.getOrElse { error -> BackupResult.Failed(error.reason()) }

    private fun Throwable.reason(): String = this::class.simpleName ?: "erreur"
}
