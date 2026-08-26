package tg.goddivor.jobcalender.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.data.backup.BackupResult
import tg.goddivor.jobcalender.ui.format.today
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onBack: () -> Unit,
    viewModel: DataViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fileName = stringResource(R.string.data_file_name, today().toString())

    // The document picker keeps the app free of any storage permission and of any path of its own.
    val createFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(JSON_MIME),
    ) { uri -> uri?.let(viewModel::export) }
    val openFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::import) }

    Scaffold(
        topBar = { SettingsTopBar(stringResource(R.string.settings_section_data), onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel(stringResource(R.string.data_section_content))
            PreferenceRow(
                title = stringResource(
                    R.string.settings_data_summary,
                    state.applicationCount,
                    state.eventCount,
                ),
                subtitle = stringResource(R.string.data_stored_locally),
            )

            SectionLabel(stringResource(R.string.data_section_backup))
            PreferenceRow(
                title = stringResource(R.string.data_export),
                subtitle = stringResource(R.string.data_export_sub),
                onClick = { if (!state.busy) createFile.launch(fileName) },
            )
            PreferenceRow(
                title = stringResource(R.string.data_import),
                subtitle = stringResource(R.string.data_import_sub),
                onClick = { if (!state.busy) openFile.launch(arrayOf(JSON_MIME)) },
            )

            state.lastResult?.let { BackupResultLine(it) }
        }
    }
}

@Composable
private fun BackupResultLine(result: BackupResult) {
    val text = when (result) {
        is BackupResult.Written ->
            stringResource(R.string.data_exported, result.applications, result.events)
        is BackupResult.Restored ->
            stringResource(R.string.data_imported, result.applications, result.events)
        BackupResult.Unreadable -> stringResource(R.string.data_unreadable)
        is BackupResult.Failed -> stringResource(R.string.data_failed, result.reason)
    }
    val failed = result is BackupResult.Unreadable || result is BackupResult.Failed
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (failed) JobCalenderTheme.semantic.danger else JobCalenderTheme.semantic.success,
        modifier = Modifier.padding(horizontal = ROW_PADDING, vertical = 6.dp),
    )
}

private const val JSON_MIME = "application/json"
