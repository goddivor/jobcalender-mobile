package tg.goddivor.jobcalender

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tg.goddivor.jobcalender.data.seed.SeedImporter
import javax.inject.Inject

@HiltAndroidApp
class JobCalenderApp : Application() {

    @Inject
    lateinit var seedImporter: SeedImporter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // First launch fills the database from the owner's real export. A failure here must not
        // take the app down: an empty calendar is recoverable, a crash loop is not.
        scope.launch {
            runCatching { seedImporter.importIfEmpty() }
                .onSuccess { report ->
                    if (report != null) {
                        Log.i(TAG, "Seeded ${report.applications} applications, ${report.events} events")
                    }
                }
                .onFailure { Log.e(TAG, "Seed import failed", it) }
        }
    }

    private companion object {
        const val TAG = "JobCalender"
    }
}
