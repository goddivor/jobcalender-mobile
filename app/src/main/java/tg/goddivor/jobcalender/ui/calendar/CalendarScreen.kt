package tg.goddivor.jobcalender.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventWithApplication
import tg.goddivor.jobcalender.ui.component.MeetingIcon
import tg.goddivor.jobcalender.ui.component.Pill
import tg.goddivor.jobcalender.ui.format.hhmm
import tg.goddivor.jobcalender.ui.format.label
import tg.goddivor.jobcalender.ui.format.monthYear
import tg.goddivor.jobcalender.ui.format.relativeDays
import tg.goddivor.jobcalender.ui.format.short
import tg.goddivor.jobcalender.ui.theme.JobCalenderTheme
import tg.goddivor.jobcalender.ui.theme.dotColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val DAYS_IN_WEEK = 7
private const val MAX_DOTS = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onOpenApplication: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_calendar)) },
                actions = {
                    IconButton(onClick = viewModel::goToToday) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.calendar_today))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item(key = "next") { NextAppointmentCard(state.next, onOpenApplication) }
            item(key = "grid") {
                MonthHeader(
                    label = state.month.atDay(1).monthYear(),
                    onPrevious = { viewModel.showMonth(state.month.minusMonths(1)) },
                    onNext = { viewModel.showMonth(state.month.plusMonths(1)) },
                )
                MonthGrid(state = state, onSelectDay = viewModel::selectDay)
            }
            if (state.conflictCountOnSelectedDay > 0) {
                item(key = "conflict") { ConflictBanner(state.conflictCountOnSelectedDay) }
            }
            item(key = "dayHeader") { DayHeader(state.selectedDay, state.dayEvents.size) }
            if (state.dayEvents.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.calendar_empty_day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
                    )
                }
            }
            items(state.dayEvents, key = { it.event.id }) { entry ->
                EventRow(
                    entry = entry,
                    inConflict = entry.event.id in state.conflictingIds,
                    onClick = { onOpenApplication(entry.application.id) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item(key = "tail") { Box(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun NextAppointmentCard(next: NextAppointment?, onOpen: (String) -> Unit) {
    if (next == null) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = stringResource(R.string.calendar_no_next),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val event = next.entry.event
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onOpen(next.entry.application.id) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.calendar_next_appointment).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = next.entry.application.employer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = next.entry.application.position,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = event.time?.hhmm() ?: stringResource(R.string.event_time_none),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = event.date.short(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(bottom = 3.dp),
                )
                Pill(
                    text = relativeDays(next.daysAway),
                    content = JobCalenderTheme.semantic.warning,
                    container = JobCalenderTheme.semantic.warningContainer,
                )
            }
            event.mode?.let { mode ->
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    MeetingIcon(link = event.link, mode = mode)
                    Text(stringResource(mode.label), style = MaterialTheme.typography.bodySmall)
                    if (event.link == null) {
                        Text(
                            text = stringResource(R.string.event_link_missing),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.replaceFirstChar { it.titlecase(Locale.FRENCH) },
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_previous_month))
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month))
        }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onSelectDay: (LocalDate) -> Unit) {
    val weekDays = remember { DayOfWeek.entries }
    val firstDay = state.month.atDay(1)
    val leadingBlanks = weekDays.indexOf(firstDay.dayOfWeek)
    val length = state.month.lengthOfMonth()
    val cells = leadingBlanks + length
    val rows = (cells + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK

    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.FRENCH).uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                )
            }
        }
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(DAYS_IN_WEEK) { column ->
                    val index = row * DAYS_IN_WEEK + column
                    val dayNumber = index - leadingBlanks + 1
                    if (dayNumber in 1..length) {
                        val date = state.month.atDay(dayNumber)
                        DayCell(
                            date = date,
                            marks = state.marks[date],
                            isToday = date == state.today,
                            isSelected = date == state.selectedDay,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectDay(date) },
                        )
                    } else {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    marks: DayMarks?,
    isToday: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val danger = JobCalenderTheme.semantic.danger
    Box(
        modifier = modifier.padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier)
                .then(
                    when {
                        // The ring is what makes 24 August impossible to miss from the grid alone.
                        marks?.hasConflict == true -> Modifier.border(2.dp, danger, CircleShape)
                        isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else -> Modifier
                    },
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val isPast = date.isBefore(LocalDate.now())
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            marks?.let { dayMarks ->
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    dayMarks.types.take(MAX_DOTS).forEach { type ->
                        val colour = if (isSelected) MaterialTheme.colorScheme.onPrimary else type.dotColor()
                        Box(Modifier.size(5.dp).clip(CircleShape).background(colour))
                    }
                }
            }
        }
        // Outside the circular clip on purpose: a top-right corner falls outside a circle, so a
        // counter drawn inside it is silently cut away.
        if (marks != null && marks.count > MAX_DOTS) {
            Text(
                text = marks.count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else danger,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 2.dp),
            )
        }
    }
}

@Composable
private fun ConflictBanner(count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = JobCalenderTheme.semantic.dangerContainer,
        contentColor = JobCalenderTheme.semantic.danger,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.calendar_conflict_banner, count),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = day.short(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (count > 0) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Text(count.toString()) }
        }
    }
}

@Composable
private fun EventRow(entry: EventWithApplication, inConflict: Boolean, onClick: () -> Unit) {
    val event = entry.event
    val danger = JobCalenderTheme.semantic.danger
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (inConflict) Modifier.background(JobCalenderTheme.semantic.dangerContainer) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.size(width = 64.dp, height = 40.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = event.time?.hhmm() ?: stringResource(R.string.event_time_none),
                style = if (event.time != null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.labelSmall,
                fontWeight = if (event.time != null) FontWeight.SemiBold else FontWeight.Normal,
                color = if (event.time != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(event.type.label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (inConflict) danger else MaterialTheme.colorScheme.primary,
                )
                event.outcome?.takeIf { it == EventOutcome.MISSED }?.let {
                    Pill(stringResource(it.label), danger, JobCalenderTheme.semantic.dangerContainer)
                }
            }
            Text(text = entry.application.employer, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = entry.application.position,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val place = event.location
            if (event.mode != null || place != null) {
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (place != null) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        MeetingIcon(link = event.link, mode = event.mode, size = 16.dp)
                    }
                    Text(
                        text = place ?: event.mode?.let { stringResource(it.label) }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
