package tg.goddivor.jobcalender.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tg.goddivor.jobcalender.domain.model.Status
import tg.goddivor.jobcalender.ui.format.label
import tg.goddivor.jobcalender.ui.theme.semantic

/** A dot plus a word: the status has to be readable at a glance and in a screenshot. */
@Composable
fun StatusPill(status: Status, modifier: Modifier = Modifier) {
    val colors = status.semantic()
    Pill(
        text = stringResource(status.label),
        content = colors.content,
        container = colors.container,
        modifier = modifier,
    )
}

@Composable
fun Pill(
    text: String,
    content: Color,
    container: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(color = content)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
private fun Box(color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color),
    )
}
