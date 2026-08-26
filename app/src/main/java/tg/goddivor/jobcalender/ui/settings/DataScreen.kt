package tg.goddivor.jobcalender.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onBack: () -> Unit,
    viewModel: DataViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            if (state.pendingWrites > 0) {
                PreferenceRow(
                    title = stringResource(R.string.settings_sync_pending, state.pendingWrites),
                    subtitle = stringResource(R.string.data_pending_sub),
                )
            }
            Hint(stringResource(R.string.data_source_note))
        }
    }
}
