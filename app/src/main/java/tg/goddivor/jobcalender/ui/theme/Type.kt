package tg.goddivor.jobcalender.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

internal val JobCalenderTypography = Typography()

/** Section headers in dense lists: the same shape Anikku uses to head a group of rows. */
val Typography.sectionHeader: TextStyle
    @Composable
    get() = bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
