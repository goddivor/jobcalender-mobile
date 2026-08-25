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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.Status
import tg.goddivor.jobcalender.ui.component.DateFieldRow
import tg.goddivor.jobcalender.ui.component.EnumDropdown
import tg.goddivor.jobcalender.ui.component.TextFieldRow
import tg.goddivor.jobcalender.ui.format.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationEditScreen(
    onDone: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: ApplicationEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    val leave = { if (state.dirty) confirmDiscard = true else onDone() }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.edit_application_new else R.string.edit_application_title,
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

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val form = state.form
            TextFieldRow(
                label = stringResource(R.string.edit_employer),
                value = form.employer,
                onValueChange = { value -> viewModel.update { it.copy(employer = value) } },
                error = stringResource(R.string.edit_required_employer).takeIf { state.showEmployerError },
            )
            TextFieldRow(
                label = stringResource(R.string.edit_position),
                value = form.position,
                onValueChange = { value -> viewModel.update { it.copy(position = value) } },
                error = stringResource(R.string.edit_required_position).takeIf { state.showPositionError },
            )
            EnumDropdown(
                label = stringResource(R.string.edit_status),
                options = Status.entries,
                selected = form.status,
                optionLabel = { stringResource(it.label) },
                onSelect = { value -> value?.let { picked -> viewModel.update { it.copy(status = picked) } } },
            )
            EnumDropdown(
                label = stringResource(R.string.edit_channel),
                options = Channel.entries,
                selected = form.channel,
                optionLabel = { stringResource(it.label) },
                onSelect = { value -> value?.let { picked -> viewModel.update { it.copy(channel = picked) } } },
            )
            DateFieldRow(
                label = stringResource(R.string.edit_sent_at),
                value = form.sentAt,
                onChange = { value -> viewModel.update { it.copy(sentAt = value) } },
            )
            DateFieldRow(
                label = stringResource(R.string.edit_closing_date),
                value = form.closingDate,
                onChange = { value -> viewModel.update { it.copy(closingDate = value) } },
            )
            TextFieldRow(
                label = stringResource(R.string.edit_reference),
                value = form.reference,
                onValueChange = { value -> viewModel.update { it.copy(reference = value) } },
                optional = true,
            )
            TextFieldRow(
                label = stringResource(R.string.edit_folder),
                value = form.folder,
                onValueChange = { value -> viewModel.update { it.copy(folder = value) } },
                optional = true,
            )
            SectionTitle(stringResource(R.string.detail_section_contact))
            TextFieldRow(
                label = stringResource(R.string.edit_contact_name),
                value = form.contactName,
                onValueChange = { value -> viewModel.update { it.copy(contactName = value) } },
                optional = true,
            )
            TextFieldRow(
                label = stringResource(R.string.edit_contact_email),
                value = form.contactEmail,
                onValueChange = { value -> viewModel.update { it.copy(contactEmail = value) } },
                optional = true,
            )
            TextFieldRow(
                label = stringResource(R.string.edit_contact_phone),
                value = form.contactPhone,
                onValueChange = { value -> viewModel.update { it.copy(contactPhone = value) } },
                optional = true,
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
            title = { Text(stringResource(R.string.delete_application_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.delete_application_body,
                        state.eventCount,
                        state.eventCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onDeleted) }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 2.dp),
    )
}
