package tg.goddivor.jobcalender

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import tg.goddivor.jobcalender.ui.about.UpdateDialog
import tg.goddivor.jobcalender.ui.navigation.JobCalenderNavHost
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme

/** The single activity. Everything else is Compose navigation inside it. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Held only until the stored theme is known, so the first frame is never painted in the
        // wrong theme. Bounded all the same: a splash that outstays its welcome reads as a freeze.
        val startedAt = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            !viewModel.ready.value && SystemClock.uptimeMillis() - startedAt < MAX_HOLD_MS
        }
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(EXIT_FADE_MS)
                .withEndAction(provider::remove)
                .start()
        }

        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val update by viewModel.update.collectAsStateWithLifecycle()
            JobCalenderTheme(
                themeMode = theme.mode,
                palette = theme.palette,
                amoled = theme.amoled,
            ) {
                Surface {
                    JobCalenderNavHost()
                    // Above every screen: an update found at launch must be offered wherever the
                    // user happens to be, not only if they walk to the About screen.
                    UpdateDialog(
                        state = update,
                        onInstall = viewModel::downloadOrInstall,
                        onLater = viewModel::dismissUpdate,
                        onOpenSettings = viewModel::openInstallSettings,
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_HOLD_MS = 1_000L
        const val EXIT_FADE_MS = 300L
    }
}
