package tg.goddivor.jobcalender.ui.about

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.ui.settings.PreferenceRow
import tg.goddivor.jobcalender.ui.settings.SettingsDivider
import tg.goddivor.jobcalender.ui.settings.SettingsTopBar
import tg.goddivor.jobcalender.ui.settings.readable
import tg.goddivor.jobcalender.updates.InstallState

/**
 * The mark, a rule, then four plain rows. Version does not react to a touch; the three others lead
 * somewhere. The two icons at the foot are the only links out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenWhatsNew: () -> Unit,
    onOpenLicense: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val update by viewModel.updateState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Android draws the app icon in its own toasts, which is exactly the shape the mockup asks for.
    LaunchedEffect(update.checkedAndUpToDate) {
        if (update.checkedAndUpToDate) {
            Toast.makeText(context, R.string.about_no_update, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = { SettingsTopBar(stringResource(R.string.about_title), onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 46.dp, bottom = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // A dedicated bitmap, not R.mipmap.ic_launcher: on API 26 and up that resolves to
                // the adaptive-icon XML, which painterResource cannot load and which crashes here.
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(18.dp)),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            SettingsDivider()

            PreferenceRow(
                title = stringResource(R.string.about_version),
                subtitle = state.installedAt?.let {
                    stringResource(R.string.about_version_value, state.version, it.readable())
                } ?: stringResource(R.string.settings_version, state.version),
            )
            PreferenceRow(
                title = stringResource(
                    if (update.checking) R.string.about_checking else R.string.about_check,
                ),
                onClick = viewModel::checkForUpdate,
                trailing = if (update.checking) {
                    { CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp) }
                } else {
                    null
                },
            )
            PreferenceRow(
                title = stringResource(R.string.about_whats_new),
                onClick = onOpenWhatsNew,
            )
            PreferenceRow(
                title = stringResource(R.string.about_section_license),
                onClick = onOpenLicense,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                LinkIcon(
                    label = stringResource(R.string.about_website),
                    painter = null,
                    url = WEBSITE_URL,
                )
                LinkIcon(
                    label = stringResource(R.string.about_github),
                    painter = R.drawable.ic_github,
                    url = REPOSITORY_URL,
                )
            }
            Box(Modifier.height(8.dp))
        }
    }

    UpdateDialog(
        state = update,
        onInstall = {
            if (update.install is InstallState.ReadyToInstall) viewModel.install() else viewModel.download()
        },
        onLater = viewModel::dismissRelease,
        onOpenSettings = viewModel::openInstallSettings,
    )
}

@Composable
private fun LinkIcon(label: String, painter: Int?, url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    IconButton(
        onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp),
    ) {
        if (painter == null) {
            Icon(
                imageVector = Icons.Outlined.Public,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                painter = painterResource(painter),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

const val WEBSITE_URL = "https://goddivor.github.io/jobcalender"
const val REPOSITORY_URL = "https://github.com/goddivor/jobcalender-mobile"
