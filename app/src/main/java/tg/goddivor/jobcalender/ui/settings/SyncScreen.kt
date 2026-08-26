package tg.goddivor.jobcalender.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.data.remote.SyncResult
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme

/**
 * Two pages in one: the configuration form until a key has been exchanged for a token, then the
 * synchronisation itself. Nothing is compiled in, so an installed APK reaches no one else's data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onBack: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val key by viewModel.key.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SettingsTopBar(stringResource(R.string.settings_section_sync), onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.configured) {
                ConfiguredSection(state, viewModel)
            } else {
                ConfigurationForm(state, key, viewModel)
            }
        }
    }
}

@Composable
private fun ConfigurationForm(state: SyncUiState, key: String, viewModel: SyncViewModel) {
    Hint(stringResource(R.string.sync_intro))
    SectionLabel(stringResource(R.string.sync_section_config))

    OutlinedTextField(
        value = state.serverUrl,
        onValueChange = viewModel::setServerUrl,
        label = { Text(stringResource(R.string.sync_server_url)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING, vertical = 6.dp),
    )

    OutlinedTextField(
        value = key,
        onValueChange = viewModel::setKey,
        label = { Text(stringResource(R.string.sync_config_key)) },
        placeholder = { Text(stringResource(R.string.sync_config_key_hint)) },
        singleLine = true,
        isError = state.configError != null,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        supportingText = {
            val error = state.configError
            if (error == null) {
                Text(stringResource(R.string.sync_config_key_help))
            } else {
                Text(
                    text = when (error) {
                        ConfigError.INVALID_KEY -> stringResource(R.string.sync_err_key)
                        ConfigError.OFFLINE -> stringResource(R.string.sync_err_offline)
                        ConfigError.SERVER ->
                            stringResource(R.string.sync_err_server, state.serverErrorCode)
                    },
                    color = JobCalenderTheme.semantic.danger,
                )
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING, vertical = 6.dp),
    )

    Button(
        onClick = viewModel::fetchConfiguration,
        enabled = key.isNotBlank() && !state.configuring,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING, vertical = 10.dp),
    ) {
        if (state.configuring) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = stringResource(R.string.sync_fetch_config),
            modifier = Modifier.padding(start = if (state.configuring) 8.dp else 0.dp),
        )
    }
}

@Composable
private fun ConfiguredSection(state: SyncUiState, viewModel: SyncViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING, vertical = 10.dp),
        shape = RoundedCornerShape(14.dp),
        color = JobCalenderTheme.semantic.successContainer,
        contentColor = JobCalenderTheme.semantic.success,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.sync_config_fetched),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.sync_config_from, state.serverUrl),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }

    SectionLabel(stringResource(R.string.settings_section_sync))
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = state.lastSyncAt?.let { stringResource(R.string.settings_last_sync, it.readable()) }
                    ?: stringResource(R.string.settings_never_synced),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    Button(
        onClick = viewModel::syncNow,
        enabled = !state.syncing,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING, vertical = 10.dp),
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

    state.lastResult?.let { SyncResultLine(it) }

    SwitchRow(
        title = stringResource(R.string.settings_sync_on_launch),
        subtitle = stringResource(R.string.settings_sync_on_launch_sub),
        checked = state.syncOnLaunch,
        onCheckedChange = viewModel::setSyncOnLaunch,
    )

    SectionLabel(stringResource(R.string.sync_section_config))
    PreferenceRow(
        title = stringResource(R.string.sync_refresh_config),
        subtitle = stringResource(R.string.sync_refresh_config_sub),
        onClick = viewModel::refreshConfiguration,
    )
    PreferenceRow(
        title = stringResource(R.string.sync_reset),
        subtitle = stringResource(R.string.sync_reset_sub),
        onClick = viewModel::reset,
    )
}

@Composable
private fun SyncResultLine(result: SyncResult) {
    val text = when (result) {
        is SyncResult.Pulled -> if (result.sent > 0) {
            stringResource(
                R.string.settings_sync_pulled_and_sent,
                result.applications,
                result.events,
                result.sent,
            )
        } else {
            stringResource(R.string.settings_sync_pulled, result.applications, result.events)
        }
        is SyncResult.Blocked -> stringResource(R.string.settings_sync_blocked, result.pending)
        SyncResult.RemoteEmpty -> stringResource(R.string.settings_sync_remote_empty)
        SyncResult.NotConfigured -> stringResource(R.string.settings_sync_unavailable)
        is SyncResult.Failed -> stringResource(R.string.settings_sync_failed, result.reason)
    }
    val wrong = result is SyncResult.Failed ||
        result is SyncResult.Blocked ||
        result is SyncResult.RemoteEmpty
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (wrong) JobCalenderTheme.semantic.danger else JobCalenderTheme.semantic.success,
        modifier = Modifier.padding(horizontal = ROW_PADDING, vertical = 2.dp),
    )
}
