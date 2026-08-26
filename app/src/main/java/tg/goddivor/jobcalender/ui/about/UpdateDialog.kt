package tg.goddivor.jobcalender.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.updates.InstallState
import tg.goddivor.jobcalender.updates.UpdateUiState

/**
 * One dialog for the whole update path: offer, download, install, and the case where Android has
 * not been told this app may install packages. Dismissing it closes it for this run only; the next
 * launch checks again and offers again, because an update nobody is reminded of never gets applied.
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onInstall: () -> Unit,
    onLater: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val release = state.release ?: return

    if (state.install is InstallState.PermissionNeeded) {
        AlertDialog(
            onDismissRequest = onLater,
            title = { Text(stringResource(R.string.update_blocked_title)) },
            text = { Text(stringResource(R.string.update_blocked_body)) },
            confirmButton = {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.update_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onLater,
        title = {
            Text(
                stringResource(
                    if (state.install is InstallState.Downloading) {
                        R.string.update_downloading
                    } else {
                        R.string.update_available_title
                    },
                ),
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.update_meta, release.version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (val install = state.install) {
                    is InstallState.Downloading -> {
                        val total = install.totalBytes.coerceAtLeast(1)
                        LinearProgressIndicator(
                            progress = { install.downloadedBytes.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.update_progress,
                                install.downloadedBytes.megabytes(),
                                install.totalBytes.megabytes(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    is InstallState.Failed -> Text(
                        text = stringResource(R.string.update_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    else -> if (release.notes.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.update_notes_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                        )
                        Text(
                            text = release.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .heightIn(max = 190.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (state.install !is InstallState.Downloading) {
                TextButton(onClick = onInstall) { Text(stringResource(R.string.update_install)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
        },
    )
}

private fun Long.megabytes(): String = String.format("%.1f Mo", this / 1_048_576.0)
