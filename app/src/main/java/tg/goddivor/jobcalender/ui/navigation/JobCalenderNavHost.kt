package tg.goddivor.jobcalender.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tg.goddivor.jobcalender.ui.applications.ApplicationDetailScreen
import tg.goddivor.jobcalender.ui.applications.ApplicationDetailViewModel
import tg.goddivor.jobcalender.ui.applications.ApplicationsScreen
import tg.goddivor.jobcalender.ui.edit.ApplicationEditScreen
import tg.goddivor.jobcalender.ui.edit.ApplicationEditViewModel
import tg.goddivor.jobcalender.ui.edit.EventEditScreen
import tg.goddivor.jobcalender.ui.edit.EventEditViewModel
import tg.goddivor.jobcalender.ui.about.AboutScreen
import tg.goddivor.jobcalender.ui.calendar.CalendarScreen
import tg.goddivor.jobcalender.ui.settings.SettingsScreen

@Composable
fun JobCalenderNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The bottom bar belongs to the three destinations only: a detail screen opens over them.
    val showBottomBar = Destination.entries.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (!showBottomBar) return@Scaffold
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    popUpTo(Destination.CALENDAR.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.label),
                            )
                        },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        // Only the bottom bar's inset is consumed here. Each screen carries its own top app bar and
        // applies the status-bar inset itself; taking innerPadding whole would count it twice and
        // leave a dead band above every title.
        NavHost(
            navController = navController,
            startDestination = Destination.CALENDAR.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Destination.CALENDAR.route) {
                CalendarScreen(
                    onOpenApplication = { id -> navController.navigate("$APPLICATION_DETAIL_ROUTE/$id") },
                )
            }
            composable(Destination.APPLICATIONS.route) {
                ApplicationsScreen(
                    onOpenApplication = { id -> navController.navigate("$APPLICATION_DETAIL_ROUTE/$id") },
                    onAddApplication = {
                        navController.navigate("$APPLICATION_EDIT_ROUTE/${ApplicationEditViewModel.NEW}")
                    },
                )
            }
            composable(
                route = "$APPLICATION_DETAIL_ROUTE/{${ApplicationDetailViewModel.ARG_ID}}",
                arguments = listOf(
                    navArgument(ApplicationDetailViewModel.ARG_ID) { type = NavType.StringType },
                ),
            ) {
                ApplicationDetailScreen(
                    onBack = navController::popBackStack,
                    onEditApplication = { id -> navController.navigate("$APPLICATION_EDIT_ROUTE/$id") },
                    onAddEvent = { applicationId, type ->
                        val suffix = type?.name ?: EventEditViewModel.NONE
                        navController.navigate(
                            "$EVENT_EDIT_ROUTE/$applicationId/${EventEditViewModel.NEW}/$suffix",
                        )
                    },
                    onEditEvent = { applicationId, eventId ->
                        navController.navigate(
                            "$EVENT_EDIT_ROUTE/$applicationId/$eventId/${EventEditViewModel.NONE}",
                        )
                    },
                )
            }
            composable(
                route = "$APPLICATION_EDIT_ROUTE/{${ApplicationEditViewModel.ARG_ID}}",
                arguments = listOf(
                    navArgument(ApplicationEditViewModel.ARG_ID) { type = NavType.StringType },
                ),
            ) {
                ApplicationEditScreen(
                    onDone = navController::popBackStack,
                    // A deleted application must not leave its own detail screen on the stack.
                    onDeleted = {
                        navController.popBackStack(Destination.APPLICATIONS.route, inclusive = false)
                    },
                )
            }
            composable(
                route = "$EVENT_EDIT_ROUTE/{${EventEditViewModel.ARG_APPLICATION_ID}}/" +
                    "{${EventEditViewModel.ARG_EVENT_ID}}/{${EventEditViewModel.ARG_TYPE}}",
                arguments = listOf(
                    navArgument(EventEditViewModel.ARG_APPLICATION_ID) { type = NavType.StringType },
                    navArgument(EventEditViewModel.ARG_EVENT_ID) { type = NavType.StringType },
                    navArgument(EventEditViewModel.ARG_TYPE) { type = NavType.StringType },
                ),
            ) {
                EventEditScreen(onDone = navController::popBackStack)
            }
            composable(Destination.SETTINGS.route) {
                SettingsScreen(onOpenAbout = { navController.navigate(ABOUT_ROUTE) })
            }
            composable(ABOUT_ROUTE) { AboutScreen(onBack = navController::popBackStack) }
        }
    }
}

private const val APPLICATION_DETAIL_ROUTE = "application"
private const val APPLICATION_EDIT_ROUTE = "application-edit"
private const val EVENT_EDIT_ROUTE = "event-edit"
private const val ABOUT_ROUTE = "about"
