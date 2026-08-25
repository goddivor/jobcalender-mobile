package tg.goddivor.jobcalender.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.data.remote.SyncResult
import tg.goddivor.jobcalender.ui.format.LOME
import tg.goddivor.jobcalender.ui.format.hhmm
import tg.goddivor.jobcalender.ui.format.short
import tg.goddivor.jobcalender.ui.format.today
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme
import tg.goddivor.jobcalender.ui.theme.ThemeMode
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel(stringResource(R.string.settings_section_sync))
            SyncBanner(state)

            Button(
                onClick = viewModel::syncNow,
                enabled = state.syncPossible && !state.syncing,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                if (state.syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = stringResource(
                        if (state.syncing) R.string.settings_syncing else R.string.settings_sync_now,
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            state.lastResult?.let { ResultLine(it) }

            if (!state.syncPossible) {
                Hint(stringResource(R.string.settings_sync_unavailable))
            }
            Hint(stringResource(R.string.settings_offline_note))

            SwitchRow(
                title = stringResource(R.string.settings_sync_on_launch),
                subtitle = stringResource(R.string.settings_sync_on_launch_sub),
                checked = state.syncOnLaunch,
                onCheckedChange = viewModel::setSyncOnLaunch,
            )

            SectionLabel(stringResource(R.string.settings_section_appearance))
            ThemeRow(current = state.themeMode, onPick = viewModel::setThemeMode)
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_sub),
                checked = state.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )

            SectionLabel(stringResource(R.string.settings_section_data))
            InfoRow(
                title = stringResource(
                    R.string.settings_data_summary,
                    state.applicationCount,
                    state.eventCount,
                ),
            )

            SectionLabel(stringResource(R.string.settings_section_about))
            InfoRow(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.settings_version, state.version),
            )
            Box(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SyncBanner(state: SettingsUiState) {
    val last = state.lastSyncAt
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (last == null) {
            JobCalenderTheme.semantic.warningContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (last == null) {
            JobCalenderTheme.semantic.warning
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = last?.let { stringResource(R.string.settings_last_sync, it.readable()) }
                    ?: stringResource(R.string.settings_never_synced),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ResultLine(result: SyncResult) {
    val text = when (result) {
        is SyncResult.Pushed -> stringResource(R.string.settings_sync_pushed, result.applications, result.events)
        is SyncResult.Pulled -> stringResource(R.string.settings_sync_pulled, result.applications, result.events)
        SyncResult.AlreadyUpToDate -> stringResource(R.string.settings_sync_uptodate)
        SyncResult.NotConfigured -> stringResource(R.string.settings_sync_unavailable)
        is SyncResult.Failed -> stringResource(R.string.settings_sync_failed, result.reason)
    }
    val colour = if (result is SyncResult.Failed) {
        JobCalenderTheme.semantic.danger
    } else {
        JobCalenderTheme.semantic.success
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colour,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

@Composable
private fun ThemeRow(current: ThemeMode, onPick: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemeSwatch(ThemeMode.LIGHT, R.string.theme_light, Color(0xFFFDFCFF), Color(0xFF191C20), current, onPick, Modifier.weight(1f))
        ThemeSwatch(ThemeMode.DARK, R.string.theme_dark, Color(0xFF111318), Color(0xFFE2E2E9), current, onPick, Modifier.weight(1f))
        ThemeSwatch(ThemeMode.AMOLED, R.string.theme_amoled, Color(0xFF000000), Color(0xFFE2E2E9), current, onPick, Modifier.weight(1f))
    }
    SwitchRow(
        title = stringResource(R.string.theme_follow_system),
        subtitle = stringResource(R.string.settings_follow_system_sub),
        checked = current == ThemeMode.FOLLOW_SYSTEM,
        onCheckedChange = { on -> onPick(if (on) ThemeMode.FOLLOW_SYSTEM else ThemeMode.DARK) },
    )
}

@Composable
private fun ThemeSwatch(
    mode: ThemeMode,
    labelRes: Int,
    background: Color,
    foreground: Color,
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Every swatch keeps an outline, including the light one, which would otherwise be invisible
    // against a light surface.
    val selected = current == mode
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onPick(mode) }
            .padding(9.dp),
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (mode == ThemeMode.LIGHT) Color(0xFF0A66EE) else Color(0xFFA8C8FF)),
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A quiet line under a control, for the thing the control cannot say by itself. */
@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** "aujourd'hui à 21h04", in Lomé time like everything else the user reads. */
@Composable
private fun Instant.readable(): String {
    val moment = atZone(LOME)
    val day = moment.toLocalDate()
    val label = if (day == today()) stringResource(R.string.relative_today) else day.short()
    return "$label ${moment.toLocalTime().hhmm()}"
}
