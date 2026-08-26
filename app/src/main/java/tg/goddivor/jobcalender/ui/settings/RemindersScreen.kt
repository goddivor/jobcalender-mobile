package tg.goddivor.jobcalender.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.reminders.ReminderNotifier
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(ReminderNotifier.canPost(context)) }
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsAllowed = granted }

    Scaffold(
        topBar = { SettingsTopBar(stringResource(R.string.settings_section_reminders), onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SwitchRow(
                title = stringResource(R.string.settings_reminder_day_before),
                subtitle = stringResource(R.string.settings_reminder_day_before_sub),
                checked = state.dayBefore,
                onCheckedChange = viewModel::setDayBefore,
            )
            SwitchRow(
                title = stringResource(R.string.settings_reminder_hour_before),
                subtitle = stringResource(R.string.settings_reminder_hour_before_sub),
                checked = state.hourBefore,
                onCheckedChange = viewModel::setHourBefore,
            )
            SwitchRow(
                title = stringResource(R.string.settings_reminder_closing),
                subtitle = stringResource(R.string.settings_reminder_closing_sub),
                checked = state.closing,
                onCheckedChange = viewModel::setClosing,
            )

            if (!notificationsAllowed) {
                Banner(
                    text = stringResource(R.string.settings_notifications_blocked),
                    colour = JobCalenderTheme.semantic.danger,
                    container = JobCalenderTheme.semantic.dangerContainer,
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        TextButton(onClick = { askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text(stringResource(R.string.settings_notifications_allow))
                        }
                    }
                }
            }

            if (!state.exactAlarmsAllowed) {
                Banner(
                    text = stringResource(R.string.settings_exact_alarms_blocked),
                    colour = JobCalenderTheme.semantic.warning,
                    container = JobCalenderTheme.semantic.warningContainer,
                )
            }
        }
    }
}

@Composable
private fun Banner(
    text: String,
    colour: androidx.compose.ui.graphics.Color,
    container: androidx.compose.ui.graphics.Color,
    action: @Composable (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING, vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = colour,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodySmall)
            action?.invoke()
        }
    }
}
