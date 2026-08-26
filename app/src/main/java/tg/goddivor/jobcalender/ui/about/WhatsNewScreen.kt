package tg.goddivor.jobcalender.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.ui.format.LOME
import tg.goddivor.jobcalender.ui.format.long

/**
 * The whole published history, newest first, and one action: there is nothing to do here but read.
 */
@Composable
fun WhatsNewScreen(
    onClose: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val state by viewModel.whatsNew.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadWhatsNew() }

    // No app bar here, so the window insets have to be applied by hand or the title lands under
    // the status bar and the button under the gesture handle.
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.NewReleases,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 26.dp, bottom = 16.dp).size(44.dp),
            )
            Text(
                text = stringResource(R.string.about_whats_new),
                style = MaterialTheme.typography.headlineMedium,
            )
            state.releases.firstOrNull()?.let { latest ->
                Text(
                    text = stringResource(
                        R.string.about_whats_new_versions,
                        latest.version,
                        state.installedVersion,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            when {
                state.loading -> CircularProgressIndicator(
                    modifier = Modifier.padding(top = 32.dp).size(28.dp),
                    strokeWidth = 3.dp,
                )
                state.releases.isEmpty() -> Text(
                    text = stringResource(R.string.about_whats_new_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp),
                )
                else -> state.releases.forEach { release ->
                    Text(
                        text = "v${release.version}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 26.dp),
                    )
                    release.publishedAt?.let {
                        Text(
                            text = it.atZone(LOME).toLocalDate().long(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = release.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(stringResource(R.string.action_ok))
        }
    }
}
