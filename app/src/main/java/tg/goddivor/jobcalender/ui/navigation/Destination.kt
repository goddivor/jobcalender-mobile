package tg.goddivor.jobcalender.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector
import tg.goddivor.jobcalender.R

/**
 * The three bottom destinations. An application detail opens over them and is not one of them.
 */
enum class Destination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    CALENDAR("calendar", R.string.nav_calendar, Icons.Filled.CalendarMonth),
    APPLICATIONS("applications", R.string.nav_applications, Icons.Filled.WorkOutline),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}
