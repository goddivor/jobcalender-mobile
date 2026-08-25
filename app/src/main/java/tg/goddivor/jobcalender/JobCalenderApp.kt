package tg.goddivor.jobcalender

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import tg.goddivor.jobcalender.data.remote.SyncEngine
import tg.goddivor.jobcalender.data.repository.ApplicationRepository
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.data.seed.SeedImporter
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltAndroidApp
class JobCalenderApp : Application() {

    @Inject
    lateinit var seedImporter: SeedImporter

    @Inject
    lateinit var syncEngine: SyncEngine

    @Inject
    lateinit var syncSettings: SyncSettings

    @Inject
    lateinit var applications: ApplicationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // First launch fills the database from the owner's real export. A failure here must not
        // take the app down: an empty calendar is recoverable, a crash loop is not.
        scope.launch { restoreOrSeed() }
    }

    /**
     * Order matters on a fresh install: try the cloud first, seed only if it has nothing.
     *
     * Seeding first and syncing after would push the starter dataset over whatever the cloud held,
     * so reinstalling the app would quietly destroy months of tracking. The seed exists to fill a
     * genuinely empty database, never to overwrite a real one.
     */
    private suspend fun restoreOrSeed() {
        val empty = runCatching { applications.count() == 0 }.getOrDefault(false)

        if (empty) {
            runCatching { syncEngine.sync() }
                .onSuccess { Log.i(TAG, "First launch sync: $it") }
                .onFailure { Log.w(TAG, "First launch sync failed", it) }
        }

        runCatching { seedImporter.importIfEmpty() }
            .onSuccess { report ->
                if (report != null) {
                    Log.i(TAG, "Seeded ${report.applications} applications, ${report.events} events")
                }
            }
            .onFailure { Log.e(TAG, "Seed import failed", it) }

        if (!empty) syncOnLaunchIfStale()
    }

    /**
     * Syncing at launch is a convenience, not a requirement: it only runs when the last one is over
     * a day old, and a failure never surfaces here. The database is local and authoritative.
     */
    private suspend fun syncOnLaunchIfStale() {
        val stored = syncSettings.state.first()
        if (!stored.syncOnLaunch) return

        val last = stored.lastSyncAt
        if (last != null && Duration.between(last, Instant.now()) < STALE_AFTER) return

        runCatching { syncEngine.sync() }
            .onSuccess { Log.i(TAG, "Launch sync: $it") }
            .onFailure { Log.w(TAG, "Launch sync failed", it) }
    }

    private companion object {
        const val TAG = "JobCalender"
        val STALE_AFTER: Duration = Duration.ofHours(24)
    }
}
