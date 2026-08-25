package tg.goddivor.jobcalender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import dagger.hilt.android.AndroidEntryPoint
import tg.goddivor.jobcalender.ui.navigation.JobCalenderNavHost
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme

/** The single activity. Everything else is Compose navigation inside it. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            JobCalenderTheme {
                Surface {
                    JobCalenderNavHost()
                }
            }
        }
    }
}
