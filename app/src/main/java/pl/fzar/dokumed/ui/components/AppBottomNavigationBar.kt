package pl.fzar.dokumed.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import pl.fzar.dokumed.R

// Define sealed class for Bottom Navigation Items using DrawableRes
sealed class BottomNavItem(val route: String, val labelResId: Int, @DrawableRes val iconResId: Int) {
    object Records : BottomNavItem("records", R.string.bottom_nav_records, R.drawable.ic_filter_list) // Use filter_list for records
    object Statistics : BottomNavItem("statistics", R.string.bottom_nav_statistics, R.drawable.ic_show_chart) // Use show_chart for statistics
    object Export : BottomNavItem("export", R.string.bottom_nav_export, R.drawable.ic_export_records) // Use export_records for export
    object Profile : BottomNavItem("profile", R.string.bottom_nav_statistics, R.drawable.ic_person) // Added Profile
}

val bottomNavItems = listOf(
    BottomNavItem.Records,
    BottomNavItem.Statistics,
    BottomNavItem.Export,
    BottomNavItem.Profile // Added Profile
)

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                // Use painterResource for drawable icons
                icon = { Icon(painterResource(id = screen.iconResId), contentDescription = stringResource(screen.labelResId)) },
                label = { Text(stringResource(screen.labelResId)) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}
