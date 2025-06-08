package pl.fzar.dokumed.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import pl.fzar.dokumed.navigation.Routes

// Define a data class for Bottom Navigation Screen items
data class BottomNavScreen(
    val route: String,
    @StringRes val labelResId: Int,
    @DrawableRes val iconResId: Int
)

// List of bottom navigation screens using routes from Routes.kt
val bottomNavScreens = listOf(
    BottomNavScreen(Routes.RECORDS, R.string.bottom_nav_records, R.drawable.ic_filter_list),
    BottomNavScreen(Routes.STATISTICS, R.string.bottom_nav_statistics, R.drawable.ic_show_chart),
    BottomNavScreen(Routes.EXPORT, R.string.bottom_nav_export, R.drawable.ic_export_records),
    BottomNavScreen(Routes.PROFILE_SETTINGS, R.string.bottom_nav_settings, R.drawable.ic_person) // Corrected route to PROFILE_SETTINGS
)

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavScreens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(painterResource(id = screen.iconResId), contentDescription = stringResource(screen.labelResId)) },
                label = { Text(stringResource(screen.labelResId)) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
