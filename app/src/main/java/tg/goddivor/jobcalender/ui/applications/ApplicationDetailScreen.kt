package tg.goddivor.jobcalender.ui.applications

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status
import tg.goddivor.jobcalender.ui.component.ContactActions
import tg.goddivor.jobcalender.ui.component.Pill
import tg.goddivor.jobcalender.ui.component.StatusPill
import tg.goddivor.jobcalender.ui.component.launch
import tg.goddivor.jobcalender.ui.format.hhmm
import tg.goddivor.jobcalender.ui.format.label
import tg.goddivor.jobcalender.ui.format.long
import tg.goddivor.jobcalender.ui.format.short
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    onBack: () -> Unit,
    onEditApplication: (String) -> Unit,
    onAddEvent: (String, EventType?) -> Unit,
    onEditEvent: (String, String) -> Unit,
    viewModel: ApplicationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickingStatus by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
                actions = {
                    state.application?.let { current ->
                        IconButton(onClick = { onEditApplication(current.id) }) {
                            Icon(Icons.Filled.Edit, stringResource(R.string.action_edit))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val application = state.application
        if (application == null) {
            if (state.loaded) {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.detail_not_found))
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                Text(application.employer, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = application.position,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusPill(application.status)
                    Text(
                        text = application.reference ?: stringResource(R.string.detail_reference_none),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // The button stays on screen while the link is missing, disabled, with the reason
            // written underneath. An absent button says nothing; a greyed one says why.
            if (state.nextLink != null || state.awaitingLink) {
                val link = state.nextLink
                Button(
                    onClick = { link?.let { context.launch(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } },
                    enabled = link != null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.action_open_link),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (state.awaitingLink) {
                Text(
                    text = stringResource(R.string.detail_link_not_received),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Both buttons share one height and one type size, and neither wraps: side by side,
            // a two-line label makes one button visibly taller than the other.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onAddEvent(application.id, null) },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text(
                        text = stringResource(R.string.action_add_event),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
                OutlinedButton(
                    onClick = { pickingStatus = true },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_change_status),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }

            SectionLabel(stringResource(R.string.detail_section_timeline))
            if (state.timeline.isEmpty()) {
                Text(
                    text = stringResource(R.string.detail_no_event),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            state.timeline.forEachIndexed { index, node ->
                TimelineRow(
                    node = node,
                    isLast = index == state.timeline.lastIndex,
                    onClick = (node as? TimelineNode.Real)?.let { real ->
                        { onEditEvent(application.id, real.event.id) }
                    },
                )
            }

            SectionLabel(stringResource(R.string.detail_section_contact))
            if (application.contactName == null && application.contactEmail == null && application.contactPhone == null) {
                Text(
                    text = stringResource(R.string.detail_no_contact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            } else {
                application.contactName?.let { Fact(stringResource(R.string.detail_contact_name), it) }
                application.contactEmail?.let { Fact(stringResource(R.string.detail_contact_email), it) }
                application.contactPhone?.let { Fact(stringResource(R.string.detail_contact_phone), it) }
                ContactActions(
                    email = application.contactEmail,
                    phone = application.contactPhone,
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (application.contactPhone == null) {
                    Text(
                        text = stringResource(R.string.detail_no_phone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            SectionLabel(stringResource(R.string.detail_section_tracking))
            Fact(stringResource(R.string.detail_channel), stringResource(application.channel.label))
            Fact(
                label = stringResource(R.string.detail_sent_on),
                value = application.sentAt?.long() ?: stringResource(R.string.application_never_sent),
                muted = application.sentAt == null,
            )
            Fact(
                label = stringResource(R.string.detail_closing),
                value = application.closingDate?.long() ?: stringResource(R.string.detail_closing_none),
                muted = application.closingDate == null,
            )
            application.folder?.let {
                Fact(stringResource(R.string.detail_folder), it, monospace = true, hint = stringResource(R.string.detail_folder_hint))
            }
            application.note?.let { Fact(stringResource(R.string.detail_note), it) }

            Box(Modifier.height(28.dp))
        }
    }

    if (pickingStatus) {
        StatusPicker(
            current = state.application?.status ?: Status.DRAFT,
            onPick = { status ->
                pickingStatus = false
                viewModel.changeStatus(status) { type ->
                    state.application?.let { onAddEvent(it.id, type) }
                }
            },
            onDismiss = { pickingStatus = false },
        )
    }
}

@Composable
private fun StatusPicker(
    current: Status,
    onPick: (Status) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_change_status)) },
        text = {
            Column {
                // Every status is offered, in progression order, with none disabled: a real
                // application jumped from acknowledged straight to test, and the app must not
                // pretend that path does not exist.
                Status.entries.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(status) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatusPill(status)
                        if (status == current) {
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun Fact(label: String, value: String, muted: Boolean = false, monospace: Boolean = false, hint: String? = null) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(96.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (monospace) FontFamily.Monospace else null,
                    color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                hint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(node: TimelineNode, isLast: Boolean, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = 16.dp),
    ) {
        Column(
            modifier = Modifier.width(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val colour = nodeColour(node)
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(colour),
            )
            if (!isLast) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .height(46.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp, bottom = 18.dp).weight(1f)) {
            when (node) {
                is TimelineNode.Real -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(node.event.type.label).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (node.isUpcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        node.event.outcome?.takeIf { it != EventOutcome.PENDING }?.let { outcome ->
                            val semantic = JobCalenderTheme.semantic
                            val pair = when (outcome) {
                                EventOutcome.MISSED -> semantic.danger to semantic.dangerContainer
                                EventOutcome.CANCELLED -> semantic.neutral to semantic.neutralContainer
                                else -> semantic.success to semantic.successContainer
                            }
                            Pill(stringResource(outcome.label), pair.first, pair.second)
                        }
                    }
                    Text(
                        text = buildString {
                            append(node.event.date.short())
                            node.event.time?.let { append(" · ").append(it.hhmm()) }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    node.event.mode?.let { mode ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(mode.label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (node.event.link == null) {
                                Text(
                                    text = stringResource(R.string.event_link_missing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    node.event.location?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is TimelineNode.Sent -> {
                    Text(
                        text = stringResource(R.string.timeline_sent).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = node.date.short(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.timeline_sent_via, stringResource(node.channel.label)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun nodeColour(node: TimelineNode): Color = when {
    node is TimelineNode.Real && node.event.outcome == EventOutcome.MISSED -> JobCalenderTheme.semantic.danger
    node is TimelineNode.Real && node.isUpcoming -> MaterialTheme.colorScheme.primary
    else -> JobCalenderTheme.semantic.neutral
}
