package com.gityou.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.gityou.app.ui.screens.home.HomeScreen
import com.gityou.app.ui.screens.issue.IssueDetailScreen
import com.gityou.app.ui.screens.issue.NewIssueScreen
import com.gityou.app.ui.screens.notifications.NotificationsScreen
import com.gityou.app.ui.screens.profile.ProfileScreen
import com.gityou.app.ui.screens.repo.RepoScreen
import com.gityou.app.ui.screens.search.SearchScreen
import com.gityou.app.ui.screens.settings.SettingsScreen

fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun GitYouNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNotificationsEntered: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Route.HOME,
        modifier = modifier,
    ) {
        composable(Route.HOME) {
            HomeScreen(
                onOpenRepo = { owner, name -> navController.navigate(Route.repo(owner, name)) },
                onOpenSettings = { navController.navigate(Route.SETTINGS) },
            )
        }
        composable(Route.SEARCH) {
            SearchScreen(
                onOpenRepo = { owner, name -> navController.navigate(Route.repo(owner, name)) },
            )
        }
        composable(Route.NOTIFICATIONS) {
            LaunchedEffect(Unit) { onNotificationsEntered() }
            NotificationsScreen(
                onOpenIssue = { owner, name, number, isPull ->
                    navController.navigate(Route.issue(owner, name, number, isPull))
                },
                onOpenRepo = { owner, name -> navController.navigate(Route.repo(owner, name)) },
            )
        }
        composable(Route.PROFILE) {
            ProfileScreen(
                onOpenRepo = { owner, name -> navController.navigate(Route.repo(owner, name)) },
            )
        }
        composable(Route.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.REPO) { entry ->
            val owner = entry.arguments?.getString("owner").orEmpty()
            val name = entry.arguments?.getString("name").orEmpty()
            RepoScreen(
                owner = owner,
                repoName = name,
                onBack = { navController.popBackStack() },
                onOpenIssue = { number, isPull ->
                    navController.navigate(Route.issue(owner, name, number, isPull))
                },
                onNewIssue = { navController.navigate(Route.newIssue(owner, name)) },
            )
        }
        composable(
            Route.ISSUE,
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("number") { type = NavType.IntType },
                navArgument("isPull") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val owner = entry.arguments?.getString("owner").orEmpty()
            val name = entry.arguments?.getString("name").orEmpty()
            val number = entry.arguments?.getInt("number") ?: 0
            val isPull = entry.arguments?.getBoolean("isPull") ?: false
            IssueDetailScreen(
                owner = owner,
                repoName = name,
                number = number,
                isPull = isPull,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.NEW_ISSUE) { entry ->
            val owner = entry.arguments?.getString("owner").orEmpty()
            val name = entry.arguments?.getString("name").orEmpty()
            NewIssueScreen(
                owner = owner,
                repoName = name,
                onBack = { navController.popBackStack() },
                onCreated = { number ->
                    navController.navigate(Route.issue(owner, name, number, false)) {
                        popUpTo(Route.NEW_ISSUE) { inclusive = true }
                    }
                },
            )
        }
    }
}
