package tg.goddivor.jobcalender.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R

/**
 * A table of contents, not a control panel: the app mark, a rule, then one entry per domain. The
 * bottom bar already names this destination, so there is no title above the mark.
 */
@Composable
fun SettingsScreen(
    onOpenAppearance: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenData: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // A dedicated bitmap, not R.mipmap.ic_launcher: on API 26 and up that resolves to the
            // adaptive-icon XML, which painterResource cannot load and which crashes this screen.
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(15.dp)),
            )
        }
        SettingsDivider()

        PreferenceRow(
            title = stringResource(R.string.settings_section_appearance),
            subtitle = stringResource(R.string.settings_appearance_sub),
            icon = Icons.Outlined.Palette,
            onClick = onOpenAppearance,
        )
        PreferenceRow(
            title = stringResource(R.string.settings_section_sync),
            subtitle = when {
                !state.syncConfigured -> stringResource(R.string.settings_sync_sub_none)
                else -> stringResource(
                    R.string.settings_sync_sub_configured,
                    state.lastSyncAt?.readable() ?: stringResource(R.string.settings_never_synced),
                )
            },
            icon = Icons.Outlined.Sync,
            onClick = onOpenSync,
        )
        PreferenceRow(
            title = stringResource(R.string.settings_section_reminders),
            subtitle = stringResource(R.string.settings_reminders_sub),
            icon = Icons.Outlined.Notifications,
            onClick = onOpenReminders,
        )
        PreferenceRow(
            title = stringResource(R.string.settings_section_data),
            subtitle = stringResource(R.string.settings_data_sub),
            icon = Icons.Outlined.Storage,
            onClick = onOpenData,
        )
        PreferenceRow(
            title = stringResource(R.string.settings_section_about),
            subtitle = stringResource(R.string.settings_about_sub, state.version),
            icon = Icons.Outlined.Info,
            onClick = onOpenAbout,
        )
    }
}
