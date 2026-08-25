package tg.goddivor.jobcalender.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.ui.format.LOME
import tg.goddivor.jobcalender.ui.format.hhmm
import tg.goddivor.jobcalender.ui.format.long
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    singleLine: Boolean = true,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(if (optional) "$label (${stringResource(R.string.edit_optional)})" else label) },
        singleLine = singleLine,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/** One dropdown for every enum: the label always comes from a string resource. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    allowNone: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val noneLabel = stringResource(R.string.edit_event_none)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        OutlinedTextField(
            value = selected?.let { optionLabel(it) } ?: noneLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowNone) {
                DropdownMenuItem(
                    text = { Text(noneLabel) },
                    onClick = { onSelect(null); expanded = false },
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
fun DateFieldRow(
    label: String,
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    optional: Boolean = true,
) {
    var picking by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        OutlinedTextField(
            value = value?.long() ?: stringResource(R.string.edit_no_date),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(if (optional) "$label (${stringResource(R.string.edit_optional)})" else label) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { picking = true })
    }

    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDay(LOME)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(
                        state.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(LOME).toLocalDate()
                        },
                    )
                    picking = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { onChange(null); picking = false }) {
                    Text(stringResource(R.string.edit_clear))
                }
            },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFieldRow(
    label: String,
    value: LocalTime?,
    onChange: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        OutlinedTextField(
            value = value?.hhmm() ?: stringResource(R.string.event_time_none),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("$label (${stringResource(R.string.edit_optional)})") },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { picking = true })
    }

    if (picking) {
        // 24-hour, always: every invitation in this dataset is written that way.
        val state = rememberTimePickerState(
            initialHour = value?.hour ?: 9,
            initialMinute = value?.minute ?: 0,
            is24Hour = true,
        )
        Dialog(onDismissRequest = { picking = false }) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                androidx.compose.foundation.layout.Column(Modifier.padding(20.dp)) {
                    TimePicker(state = state)
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    ) {
                        TextButton(onClick = { onChange(null); picking = false }) {
                            Text(stringResource(R.string.edit_clear))
                        }
                        TextButton(onClick = {
                            onChange(LocalTime.of(state.hour, state.minute))
                            picking = false
                        }) { Text(stringResource(R.string.action_save)) }
                    }
                }
            }
        }
    }
}
