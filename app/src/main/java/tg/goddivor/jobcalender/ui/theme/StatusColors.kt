package tg.goddivor.jobcalender.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status

/**
 * Four meanings for ten statuses, on purpose: the pill says where the application stands, the
 * colour says whether it is moving. The mapping is frozen by the design unit.
 */
data class SemanticPair(val content: Color, val container: Color)

@Composable
@ReadOnlyComposable
fun Status.semantic(): SemanticPair {
    val semantic = JobCalenderTheme.semantic
    return when (this) {
        Status.DRAFT, Status.SENT, Status.ACKNOWLEDGED, Status.READ ->
            SemanticPair(semantic.neutral, semantic.neutralContainer)
        Status.SHORTLISTED, Status.TEST ->
            SemanticPair(semantic.warning, semantic.warningContainer)
        Status.INTERVIEW, Status.OFFER ->
            SemanticPair(semantic.success, semantic.successContainer)
        Status.REJECTED, Status.NO_REPLY ->
            SemanticPair(semantic.danger, semantic.dangerContainer)
    }
}

/**
 * Calendar dot families. Appointments carry the accent; milestones stay neutral; a closing date
 * warns; an outcome is either a win or a loss.
 */
@Composable
@ReadOnlyComposable
fun EventType.dotColor(): Color {
    val semantic = JobCalenderTheme.semantic
    return when (this) {
        EventType.INTERVIEW, EventType.TEST, EventType.MEETING -> primaryDot()
        EventType.ACKNOWLEDGEMENT, EventType.READ, EventType.SHORTLIST -> semantic.neutral
        EventType.DEADLINE, EventType.FOLLOW_UP -> semantic.warning
        EventType.OFFER -> semantic.success
        EventType.REJECTION -> semantic.danger
    }
}

@Composable
@ReadOnlyComposable
private fun primaryDot(): Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
