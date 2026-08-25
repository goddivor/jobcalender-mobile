package tg.goddivor.jobcalender.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.ui.component.DateFieldRow
import tg.goddivor.jobcalender.ui.component.EnumDropdown
import tg.goddivor.jobcalender.ui.component.TextFieldRow
import tg.goddivor.jobcalender.ui.component.TimeFieldRow
import tg.goddivor.jobcalender.ui.format.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    onDone: () -> Unit,
    viewModel: EventEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }
    val leave = { if (state.dirty) confirmDiscard = true else onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.edit_event_new else R.string.edit_event_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.action_delete))
                        }
                    }
                    TextButton(onClick = viewModel::save) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        val form = state.form

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            EnumDropdown(
                label = stringResource(R.string.edit_event_type),
                options = EventType.entries,
                selected = form.type,
                optionLabel = { stringResource(it.label) },
                onSelect = { value -> value?.let { picked -> viewModel.update { it.copy(type = picked) } } },
            )
            DateFieldRow(
                label = stringResource(R.string.edit_event_date),
                value = form.date,
                onChange = { value -> value?.let { picked -> viewModel.update { it.copy(date = picked) } } },
                optional = false,
            )
            TimeFieldRow(
                label = stringResource(R.string.edit_event_time),
                value = form.time,
                onChange = { value -> viewModel.update { it.copy(time = value) } },
            )
            TextFieldRow(
                label = stringResource(R.string.edit_event_duration),
                value = form.durationMinutes,
                onValueChange = { value -> viewModel.update { it.copy(durationMinutes = value.filter(Char::isDigit)) } },
                optional = true,
            )
            EnumDropdown(
                label = stringResource(R.string.edit_event_mode),
                options = EventMode.entries,
                selected = form.mode,
                optionLabel = { stringResource(it.label) },
                onSelect = { value -> viewModel.update { it.copy(mode = value) } },
                allowNone = true,
            )
            TextFieldRow(
                label = stringResource(R.string.edit_event_location),
                value = form.location,
                onValueChange = { value -> viewModel.update { it.copy(location = value) } },
                optional = true,
            )
            TextFieldRow(
                label = stringResource(R.string.edit_event_link),
                value = form.link,
                onValueChange = { value -> viewModel.update { it.copy(link = value) } },
                optional = true,
            )
            EnumDropdown(
                label = stringResource(R.string.edit_event_outcome),
                options = EventOutcome.entries,
                selected = form.outcome,
                optionLabel = { stringResource(it.label) },
                onSelect = { value -> viewModel.update { it.copy(outcome = value) } },
                allowNone = true,
            )
            TextFieldRow(
                label = stringResource(R.string.edit_note),
                value = form.note,
                onValueChange = { value -> viewModel.update { it.copy(note = value) } },
                optional = true,
                singleLine = false,
            )
            Box(Modifier.height(28.dp))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDone() }) {
                    Text(stringResource(R.string.discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.discard_keep))
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_event_title)) },
            text = { Text(stringResource(R.string.delete_event_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onDone) }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
