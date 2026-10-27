package com.diettracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.diettracker.ui.feature.history.HistoryScreen
import com.diettracker.ui.feature.home.HomeScreen
import com.diettracker.ui.feature.record.RecordScreen
import com.diettracker.ui.feature.settings.SettingsScreen
import com.diettracker.ui.feature.stats.StatsScreen

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "首页", Icons.Filled.Home),
    RECORD("record", "记录", Icons.Filled.Add),
    HISTORY("history", "历史", Icons.Filled.DateRange),
    STATS("stats", "统计", Icons.Filled.Info),
}

@Composable
fun DietTrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onOpenSettings = { navController.navigate("settings") },
                    onAddRecord = { navController.navigateToTopLevel(TopLevelDestination.RECORD.route) },
                )
            }
            composable(TopLevelDestination.RECORD.route) {
                RecordScreen(
                    onNavigateHome = { navController.navigateToTopLevel(TopLevelDestination.HOME.route) },
                )
            }
            composable(TopLevelDestination.HISTORY.route) { HistoryScreen() }
            composable(TopLevelDestination.STATS.route) { StatsScreen() }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

/** 切换到顶层目的地（与底部导航一致的返回栈行为）。 */
private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
