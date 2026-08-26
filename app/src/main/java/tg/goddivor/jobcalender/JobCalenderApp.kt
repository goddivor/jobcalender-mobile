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
import tg.goddivor.jobcalender.reminders.ReminderNotifier
import tg.goddivor.jobcalender.reminders.ReminderScheduler
import tg.goddivor.jobcalender.data.remote.SyncSettings
import tg.goddivor.jobcalender.data.seed.SeedImporter
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

    @Inject
    lateinit var reminders: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // First launch fills the database from the owner's real export. A failure here must not
        // take the app down: an empty calendar is recoverable, a crash loop is not.
        ReminderNotifier.ensureChannel(this)
        scope.launch {
            restoreOrSeed()
            // Alarms do not survive a reinstall or a data change, so they are rebuilt from what the
            // database actually holds rather than trusted to still be booked.
            runCatching { reminders.reschedule() }
                .onFailure { Log.w(TAG, "Reminder scheduling failed", it) }
        }
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

        if (!empty) refreshOnLaunch()
    }

    /**
     * The server is fed by the jobing MCP, so the copy on this device is stale the moment the owner
     * touches their machine. Refreshing on every launch is therefore the normal case, not a
     * periodic chore: there is no staleness delay. A failure never surfaces here; with no network
     * the app simply shows what it last received.
     */
    private suspend fun refreshOnLaunch() {
        if (!syncSettings.state.first().syncOnLaunch) return

        runCatching { syncEngine.sync() }
            .onSuccess { Log.i(TAG, "Launch refresh: $it") }
            .onFailure { Log.w(TAG, "Launch refresh failed", it) }
    }

    private companion object {
        const val TAG = "JobCalender"
    }
}
