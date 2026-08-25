package tg.goddivor.jobcalender.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import tg.goddivor.jobcalender.R

/** "aujourd'hui", "demain", "dans 2 jours": the answer to the first question the user asks. */
@Composable
fun relativeDays(days: Long): String = when {
    days == 0L -> stringResource(R.string.relative_today)
    days == 1L -> stringResource(R.string.relative_tomorrow)
    days == -1L -> stringResource(R.string.relative_yesterday)
    days > 0 -> pluralStringResource(R.plurals.relative_in_days, days.toInt(), days.toInt())
    else -> pluralStringResource(R.plurals.relative_days_ago, (-days).toInt(), (-days).toInt())
}
