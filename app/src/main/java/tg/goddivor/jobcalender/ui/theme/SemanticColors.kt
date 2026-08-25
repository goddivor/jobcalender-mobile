package tg.goddivor.jobcalender.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Four meanings, carried outside the Material scheme on purpose: a status must keep the same
 * meaning from one device to the next, so these never follow Material You dynamic colour.
 */
@Immutable
data class SemanticColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val neutral: Color,
    val successContainer: Color,
    val warningContainer: Color,
    val dangerContainer: Color,
    val neutralContainer: Color,
)

internal val LightSemanticColors = SemanticColors(
    success = SuccessLight,
    warning = WarningLight,
    danger = DangerLight,
    neutral = NeutralLight,
    successContainer = SuccessContainerLight,
    warningContainer = WarningContainerLight,
    dangerContainer = DangerContainerLight,
    neutralContainer = NeutralContainerLight,
)

internal val DarkSemanticColors = SemanticColors(
    success = SuccessDark,
    warning = WarningDark,
    danger = DangerDark,
    neutral = NeutralDark,
    successContainer = SuccessContainerDark,
    warningContainer = WarningContainerDark,
    dangerContainer = DangerContainerDark,
    neutralContainer = NeutralContainerDark,
)

internal val AmoledSemanticColors = DarkSemanticColors.copy(
    successContainer = SuccessContainerAmoled,
    warningContainer = WarningContainerAmoled,
    dangerContainer = DangerContainerAmoled,
    neutralContainer = NeutralContainerAmoled,
)

internal val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
