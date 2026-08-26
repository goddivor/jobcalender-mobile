package tg.goddivor.jobcalender.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.ui.format.LOME
import tg.goddivor.jobcalender.ui.format.hhmm
import tg.goddivor.jobcalender.ui.format.short
import tg.goddivor.jobcalender.ui.format.today
import java.time.Instant

/**
 * The shared furniture of the settings pages. Rows carry their hierarchy by typography alone: no
 * card, no leading icon below the root, and 24 dp of horizontal air so nothing touches the edge.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = ROW_PADDING, end = ROW_PADDING, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun PreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = ROW_PADDING, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 18.dp).size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        if (trailing != null) {
            Row(modifier = Modifier.padding(start = 16.dp)) { trailing() }
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    PreferenceRow(
        title = title,
        subtitle = subtitle,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) },
    )
}

@Composable
fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = ROW_PADDING, vertical = 6.dp),
    )
}

@Composable
fun SettingsDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

val ROW_PADDING = 24.dp

/** Lomé time, with the near days named rather than dated. Never an offset. */
@Composable
fun Instant.readable(): String {
    val moment = atZone(LOME)
    val day = moment.toLocalDate()
    val label = when (day) {
        today() -> stringResource(R.string.relative_today)
        today().minusDays(1) -> stringResource(R.string.relative_yesterday)
        else -> day.short()
    }
    return "$label ${moment.toLocalTime().hhmm()}"
}
