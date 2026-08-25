package tg.goddivor.jobcalender.ui.applications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.ui.component.StatusPill
import tg.goddivor.jobcalender.ui.component.EmptyState
import tg.goddivor.jobcalender.ui.format.label
import tg.goddivor.jobcalender.ui.format.short
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme

@Composable
fun ApplicationsScreen(
    onOpenApplication: (String) -> Unit,
    onAddApplication: () -> Unit,
    viewModel: ApplicationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ApplicationsContent(
        state = state,
        onQueryChange = viewModel::search,
        onStatusChange = viewModel::selectStatus,
        onOpenApplication = onOpenApplication,
        onAddApplication = onAddApplication,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationsContent(
    state: ApplicationsUiState,
    onQueryChange: (String) -> Unit,
    onStatusChange: (tg.goddivor.jobcalender.domain.model.Status?) -> Unit,
    onOpenApplication: (String) -> Unit,
    onAddApplication: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_applications)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddApplication,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add)) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // A filled, fully rounded field with no outline, as in applications.html: the search
            // is a resting surface, not a form control competing with the rows below it.
            TextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.applications_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.filters.forEach { filter ->
                    FilterChip(
                        selected = filter.selected,
                        onClick = { onStatusChange(filter.status) },
                        label = {
                            Text(
                                filter.status?.let { stringResource(it.label) }
                                    ?: stringResource(R.string.applications_filter_all),
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = filter.count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }

            if (state.groups.isEmpty()) {
                EmptyState(
                    message = stringResource(
                        if (state.query.isBlank()) {
                            R.string.applications_empty_filter
                        } else {
                            R.string.applications_empty_search
                        },
                    ),
                    icon = Icons.Filled.WorkOutline,
                )
                return@Column
            }

            // Room for the floating button, which would otherwise sit on top of the last row.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = "header-${group.employer}") {
                        EmployerHeader(group)
                    }
                    items(group.rows, key = { it.application.id }) { row ->
                        ApplicationRowItem(row = row, onClick = { onOpenApplication(row.application.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployerHeader(group: EmployerGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = group.employer,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Badge(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) { Text(group.rows.size.toString()) }
    }
}

@Composable
private fun ApplicationRowItem(row: ApplicationRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.application.position,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(row.application.channel.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (val closing = row.closing) {
                    is ClosingState.Upcoming -> Text(
                        text = "· " + stringResource(R.string.application_closing_on, closing.date.short()),
                        style = MaterialTheme.typography.bodySmall,
                        color = JobCalenderTheme.semantic.warning,
                    )
                    ClosingState.Past -> Text(
                        text = "· " + stringResource(R.string.application_closing_past),
                        style = MaterialTheme.typography.bodySmall,
                        color = JobCalenderTheme.semantic.danger,
                    )
                    ClosingState.None -> Unit
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusPill(row.application.status)
            Text(
                text = row.lastMovement?.short() ?: stringResource(R.string.application_never_sent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
