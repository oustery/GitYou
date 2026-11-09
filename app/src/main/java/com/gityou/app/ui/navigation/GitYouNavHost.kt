package com.gityou.app.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.gityou.app.data.util.GitHubUrls
import com.gityou.app.ui.components.LocalMarkdownLinkHandler
import com.gityou.app.ui.screens.commits.CommitsScreen
import com.gityou.app.ui.screens.commits.DiffScreen
import com.gityou.app.ui.screens.creation.NewGistScreen
import com.gityou.app.ui.screens.creation.NewRepoScreen
import com.gityou.app.ui.screens.home.HomeScreen
import com.gityou.app.ui.screens.issue.IssueDetailScreen
import com.gityou.app.ui.screens.issue.NewIssueScreen
import com.gityou.app.ui.screens.mywork.MyWorkScreen
import com.gityou.app.ui.screens.notifications.NotificationsScreen
import com.gityou.app.ui.screens.profile.ProfileScreen
import com.gityou.app.ui.screens.releases.ReleasesScreen
import com.gityou.app.ui.screens.repo.RepoPeopleScreen
import com.gityou.app.ui.screens.repo.RepoScreen
import com.gityou.app.ui.screens.search.SearchScreen
import com.gityou.app.ui.screens.settings.SettingsScreen
import com.gityou.app.ui.screens.user.UserScreen

fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/*
 * Expressive navigation motion: quiet fade effects paired with a restrained
 * spring slide. Details arrive from the right and leave back to the right;
 * nothing bounces, reading order stays stable. Specs resolve from the active
 * MotionScheme, so the "calm motion" setting covers navigation too (E-005).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GitYouNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNotificationsEntered: () -> Unit = {},
) {
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    // One link router for every markdown surface (issue bodies, comments,
    // READMEs, releases): github.com links open the matching app screen,
    // everything else keeps the default browser behaviour (G-007).
    val markdownLinkHandler: (String) -> Boolean = { url ->
        when (val target = GitHubUrls.routeLink(url)) {
            is GitHubUrls.LinkTarget.Thread -> {
                navController.navigate(
                    Route.issue(
                        target.ref.owner,
                        target.ref.repo,
                        target.ref.number,
                        target.ref.isPull,
                    ),
                )
                true
            }
            is GitHubUrls.LinkTarget.Repo -> {
                navController.navigate(Route.repo(target.owner, target.repo))
                true
            }
            is GitHubUrls.LinkTarget.User -> {
                navController.navigate(Route.user(target.login))
                true
            }
            null -> false
        }
    }
    CompositionLocalProvider(LocalMarkdownLinkHandler provides markdownLinkHandler) {
        NavHost(
            navController = navController,
            startDestination = Route.HOME,
            modifier = modifier,
            enterTransition = {
                fadeIn(effects) +
                    slideInHorizontally(spatial) { it / 8 }
            },
            exitTransition = {
                fadeOut(effects) +
                    slideOutHorizontally(spatial) { -it / 10 }
            },
            popEnterTransition = {
                fadeIn(effects) +
                    slideInHorizontally(spatial) { -it / 8 }
            },
            popExitTransition = {
                fadeOut(effects) +
                    slideOutHorizontally(spatial) { it / 10 }
            },
        ) {
            composable(Route.HOME) {
                HomeScreen(
                    onOpenRepo = { owner, name -> navController.navigate(Route.repo(owner, name)) },
                    onOpenSettings = { navController.navigate(Route.SETTINGS) },
                    onCreateRepo = { navController.navigate(Route.NEW_REPO) },
                    onOpenMyWork = { navController.navigate(Route.MY_WORK) },
                )
            }
            composable(Route.SEARCH) {
                SearchScreen(
                    onOpenRepo = { owner, name -> navController.navigate(Route.repo(owner, name)) },
                    onOpenUser = { login -> navController.navigate(Route.user(login)) },
                    onOpenIssue = { owner, name, number, isPull ->
                        navController.navigate(Route.issue(owner, name, number, isPull))
                    },
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
                    onCreateGist = { navController.navigate(Route.NEW_GIST) },
                )
            }
            composable(Route.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.MY_WORK) {
                MyWorkScreen(
                    onBack = { navController.popBackStack() },
                    onOpenIssue = { owner, name, number, isPull ->
                        navController.navigate(Route.issue(owner, name, number, isPull))
                    },
                )
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
                    onOpenCommits = { ref ->
                        navController.navigate(Route.commits(owner, name, ref))
                    },
                    onOpenUser = { login -> navController.navigate(Route.user(login)) },
                    onOpenReleases = { navController.navigate(Route.releases(owner, name)) },
                    onOpenRepoPeople = { kind -> navController.navigate(Route.repoPeople(owner, name, kind)) },
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
                    number = number,
                    isPull = isPull,
                    onBack = { navController.popBackStack() },
                    onOpenFiles = { navController.navigate(Route.pullDiff(owner, name, number)) },
                    onOpenUser = { login -> navController.navigate(Route.user(login)) },
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
            composable(
                Route.COMMITS,
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("ref") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val owner = entry.arguments?.getString("owner").orEmpty()
                val name = entry.arguments?.getString("name").orEmpty()
                CommitsScreen(
                    owner = owner,
                    repoName = name,
                    onBack = { navController.popBackStack() },
                    onOpenCommit = { sha ->
                        navController.navigate(Route.commitDiff(owner, name, sha))
                    },
                    onOpenUser = { login -> navController.navigate(Route.user(login)) },
                )
            }
            composable(
                Route.COMMIT_DIFF,
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("sha") { type = NavType.StringType },
                ),
            ) { entry ->
                DiffScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Route.PULL_DIFF,
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("number") { type = NavType.IntType },
                    navArgument("isPull") { type = NavType.BoolType; defaultValue = false },
                ),
            ) {
                DiffScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.NEW_REPO) {
                NewRepoScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { fullName ->
                        val owner = fullName.substringBefore('/')
                        val name = fullName.substringAfter('/')
                        navController.navigate(Route.repo(owner, name)) {
                            popUpTo(Route.NEW_REPO) { inclusive = true }
                        }
                    },
                )
            }
            composable(Route.NEW_GIST) {
                NewGistScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Route.USER,
                arguments = listOf(navArgument("login") { type = NavType.StringType }),
            ) {
                UserScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRepo = { owner, name -> navController.navigate(Route.repo(owner, name)) },
                    onOpenUser = { login -> navController.navigate(Route.user(login)) },
                )
            }
            composable(
                Route.RELEASES,
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                ),
            ) { entry ->
                val owner = entry.arguments?.getString("owner").orEmpty()
                val name = entry.arguments?.getString("name").orEmpty()
                ReleasesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCommits = { ref -> navController.navigate(Route.commits(owner, name, ref)) },
                )
            }
            composable(
                Route.REPO_PEOPLE,
                arguments = listOf(
                    navArgument("owner") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("kind") { type = NavType.StringType; defaultValue = "stargazers" },
                ),
            ) {
                RepoPeopleScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRepo = { owner, name -> navController.navigate(Route.repo(owner, name)) },
                    onOpenUser = { login -> navController.navigate(Route.user(login)) },
                )
            }
        }
    }
}
