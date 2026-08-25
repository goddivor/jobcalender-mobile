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
        NavHost(
            navController = navController,
            startDestination = Destination.CALENDAR.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.CALENDAR.route) { CalendarScreen() }
            composable(Destination.APPLICATIONS.route) {
                ApplicationsScreen(
                    onOpenApplication = { id -> navController.navigate("$APPLICATION_DETAIL_ROUTE/$id") },
                )
            }
            composable(
                route = "$APPLICATION_DETAIL_ROUTE/{${ApplicationDetailViewModel.ARG_ID}}",
                arguments = listOf(
                    navArgument(ApplicationDetailViewModel.ARG_ID) { type = NavType.StringType },
                ),
            ) {
                ApplicationDetailScreen(onBack = navController::popBackStack)
            }
            composable(Destination.SETTINGS.route) { SettingsScreen() }
        }
    }
}

private const val APPLICATION_DETAIL_ROUTE = "application"
