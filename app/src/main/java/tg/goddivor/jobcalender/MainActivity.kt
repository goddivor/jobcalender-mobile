package tg.goddivor.jobcalender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val update by viewModel.update.collectAsStateWithLifecycle()
            JobCalenderTheme(themeMode = theme.mode, useDynamicColor = theme.dynamicColor) {
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
}
