package com.gityou.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gityou.app.R
import com.gityou.app.data.util.DeepLinkRouting
import com.gityou.app.ui.components.AppLockScreen
import com.gityou.app.ui.components.LocalHapticsEnabled
import com.gityou.app.ui.components.LocalMarkdownFactory
import com.gityou.app.ui.components.LocalRenderMarkdown
import com.gityou.app.ui.components.MarkdownFactory
import com.gityou.app.ui.components.gyNavigationSuiteItems
import com.gityou.app.ui.navigation.GitYouNavHost
import com.gityou.app.ui.navigation.Route
import com.gityou.app.ui.navigation.navigateTopLevel
import com.gityou.app.ui.screens.auth.AuthScreen
import com.gityou.app.ui.theme.AccentPalette
import com.gityou.app.ui.theme.GitYouTheme
import com.gityou.app.ui.theme.TextScale
import com.gityou.app.ui.theme.ThemeMode

@Composable
fun GitYouApp(
    markdownFactory: MarkdownFactory,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val appState by appViewModel.appState.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val unreadCount by appViewModel.unreadCount.collectAsStateWithLifecycle()
    val locked by appViewModel.locked.collectAsStateWithLifecycle()
    val pendingRoute by appViewModel.pendingRoute.collectAsStateWithLifecycle()
    val pendingShare by appViewModel.pendingShare.collectAsStateWithLifecycle()

    // The in-app text scale multiplies the system font scale; markdown
    // views and every Material text follow it consistently.
    val systemDensity = LocalDensity.current
    val textScale = TextScale.fromKey(settings.fontScale)
    val scaledDensity = Density(
        density = systemDensity.density,
        fontScale = systemDensity.fontScale * textScale.factor,
    )

    GitYouTheme(
        themeMode = ThemeMode.fromKey(settings.themeMode),
        useDynamicColor = settings.dynamicColor,
        accent = AccentPalette.fromKey(settings.accent),
        amoled = settings.amoled,
        expressiveMotion = settings.expressiveMotion,
    ) {
        CompositionLocalProvider(
            LocalMarkdownFactory provides markdownFactory,
            LocalHapticsEnabled provides settings.hapticsEnabled,
            LocalRenderMarkdown provides settings.renderMarkdown,
            LocalDensity provides scaledDensity,
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (appState) {
                    AppViewModel.AppState.Loading -> BrandedLoading()
                    AppViewModel.AppState.SignedOut -> AuthScreen()
                    // The lock sits between the credential and the content, so
                    // nothing behind it is composed while locked (G-016).
                    is AppViewModel.AppState.SignedIn -> if (locked) {
                        AppLockScreen(onUnlocked = appViewModel::unlock)
                    } else {
                        MainScaffold(
                            unreadCount = unreadCount,
                            defaultTab = Route.defaultTabRoute(settings.defaultTab),
                            onRefreshUnread = appViewModel::refreshUnread,
                            pendingRoute = pendingRoute,
                            onPendingRouteHandled = appViewModel::consumePendingRoute,
                            pendingShare = pendingShare,
                            onPendingShareHandled = appViewModel::consumePendingShare,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    unreadCount: Int,
    defaultTab: String,
    onRefreshUnread: () -> Unit,
    pendingRoute: String?,
    onPendingRouteHandled: () -> Unit,
    pendingShare: String?,
    onPendingShareHandled: () -> Unit,
) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The suite picks bar/rail/drawer from the real window (folds included),
    // replacing the hand-rolled Row { rail; Scaffold } composition.
    val suiteState = rememberNavigationSuiteScaffoldState()
    val suiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
    val barAtBottom = suiteType == NavigationSuiteType.NavigationBar ||
        suiteType == NavigationSuiteType.ShortNavigationBarCompact ||
        suiteType == NavigationSuiteType.ShortNavigationBarMedium
    // Detail screens (repo, issue, settings) use the full height: when the
    // suite sits at the bottom it slides away; rails stay, as before.
    LaunchedEffect(currentRoute, barAtBottom) {
        if (currentRoute in Route.TOP_LEVEL || !barAtBottom) suiteState.show() else suiteState.hide()
    }

    // Honor the "default tab" setting once per sign-in; later tab switching
    // is entirely up to the user.
    LaunchedEffect(Unit) {
        if (defaultTab != Route.HOME) navController.navigateTopLevel(defaultTab)
    }

    // An App Link that arrived before sign-in is replayed here, once the graph
    // exists (G-012). It wins over the default tab: the user asked for that
    // exact screen by tapping a link.
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        navController.navigate(route)
        onPendingRouteHandled()
    }

    // "Share to GitYou" mostly carries a link (browsers append the page URL),
    // so it opens that screen; text without a GitHub link has no destination
    // the app can render and is dropped instead of opening a wrong screen.
    LaunchedEffect(pendingShare) {
        val text = pendingShare ?: return@LaunchedEffect
        val route = DeepLinkRouting.route(
            url = text,
            issueRoute = Route::issue,
            repoRoute = Route::repo,
            userRoute = Route::user,
        )
        if (route != null) navController.navigate(route)
        onPendingShareHandled()
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            gyNavigationSuiteItems(currentRoute, unreadCount) { route ->
                navController.navigateTopLevel(route)
            }
        },
        layoutType = suiteType,
        state = suiteState,
    ) {
        // The suite reserves the space next to / above the navigation itself;
        // screens manage their own scaffolds inside the NavHost.
        GitYouNavHost(
            navController = navController,
            onNotificationsEntered = onRefreshUnread,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BrandedLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(112.dp),
            )
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            LoadingIndicator()
        }
    }
}
