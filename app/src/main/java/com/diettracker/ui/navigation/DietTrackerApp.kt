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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.diettracker.ui.feature.goal.GoalSetupScreen
import com.diettracker.ui.feature.history.HistoryScreen
import com.diettracker.ui.feature.home.HomeScreen
import com.diettracker.ui.feature.record.RecordScreen
import com.diettracker.ui.feature.settings.SettingsScreen
import com.diettracker.ui.feature.stats.StatsScreen
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // 记录页未保存修改守卫：底部导航切换前询问是否放弃。
    var recordDirty by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<String?>(null) }
    var editMealId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            val leavingRecord =
                                currentDestination?.route == TopLevelDestination.RECORD.route &&
                                    destination.route != TopLevelDestination.RECORD.route
                            if (leavingRecord && recordDirty) {
                                pendingNavigation = destination.route
                            } else {
                                navController.navigateToTopLevel(destination.route)
                            }
                        },
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
                    onEditMeal = { id ->
                        editMealId = id
                        navController.navigateToTopLevel(TopLevelDestination.RECORD.route)
                    },
                    onOpenGoalSetup = { navController.navigate("goal_setup") },
                    onShowMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                )
            }
            composable(TopLevelDestination.RECORD.route) {
                RecordScreen(
                    onNavigateHome = { navController.navigateToTopLevel(TopLevelDestination.HOME.route) },
                    onShowMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                    onDirtyChange = { recordDirty = it },
                    pendingNavigation = pendingNavigation,
                    onDiscardResolved = { discard ->
                        val target = pendingNavigation
                        pendingNavigation = null
                        if (discard) {
                            recordDirty = false
                            if (target != null) navController.navigateToTopLevel(target)
                        }
                    },
                    editMealId = editMealId,
                    onEditMealHandled = { editMealId = null },
                )
            }
            composable(TopLevelDestination.HISTORY.route) { HistoryScreen() }
            composable(TopLevelDestination.STATS.route) { StatsScreen() }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditGoal = { navController.navigate("goal_setup") },
                )
            }
            composable("goal_setup") {
                GoalSetupScreen(onBack = { navController.popBackStack() })
            }
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
