package com.gityou.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import com.gityou.app.ui.components.GyBottomBar
import com.gityou.app.ui.components.GyNavigationRail
import com.gityou.app.ui.components.LocalMarkdownFactory
import com.gityou.app.ui.navigation.GitYouNavHost
import com.gityou.app.ui.navigation.Route
import com.gityou.app.ui.navigation.navigateTopLevel
import com.gityou.app.ui.screens.auth.AuthScreen
import com.gityou.app.ui.theme.AccentPalette
import com.gityou.app.ui.theme.GitYouTheme
import com.gityou.app.ui.theme.TextScale
import com.gityou.app.ui.theme.ThemeMode

private fun themeModeFrom(raw: String): ThemeMode = when (raw) {
    "light" -> ThemeMode.LIGHT
    "dark" -> ThemeMode.DARK
    else -> ThemeMode.SYSTEM
}

private fun defaultTabRoute(raw: String?): String = when (raw) {
    Route.SEARCH, Route.NOTIFICATIONS, Route.PROFILE -> raw
    else -> Route.HOME
}

@Composable
fun GitYouApp(
    windowSizeClass: WindowWidthSizeClass,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val appState by appViewModel.appState.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val unreadCount by appViewModel.unreadCount.collectAsStateWithLifecycle()

    // The in-app text scale multiplies the system font scale; markdown
    // views and every Material text follow it consistently.
    val systemDensity = LocalDensity.current
    val textScale = TextScale.fromKey(settings.fontScale)
    val scaledDensity = Density(
        density = systemDensity.density,
        fontScale = systemDensity.fontScale * textScale.factor,
    )

    GitYouTheme(
        themeMode = themeModeFrom(settings.themeMode),
        useDynamicColor = settings.dynamicColor,
        accent = AccentPalette.fromKey(settings.accent),
        amoled = settings.amoled,
        expressiveMotion = settings.expressiveMotion,
    ) {
        CompositionLocalProvider(
            LocalMarkdownFactory provides appViewModel.markdownFactory,
            LocalDensity provides scaledDensity,
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (val state = appState) {
                    AppViewModel.AppState.Loading -> BrandedLoading()
                    AppViewModel.AppState.SignedOut -> AuthScreen()
                    is AppViewModel.AppState.SignedIn -> MainScaffold(
                        windowSizeClass = windowSizeClass,
                        unreadCount = unreadCount,
                        defaultTab = defaultTabRoute(settings.defaultTab),
                        onRefreshUnread = appViewModel::refreshUnread,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    windowSizeClass: WindowWidthSizeClass,
    unreadCount: Int,
    defaultTab: String,
    onRefreshUnread: () -> Unit,
) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showRail = windowSizeClass == WindowWidthSizeClass.Expanded

    // Honor the "default tab" setting once per sign-in; later tab switching
    // is entirely up to the user.
    LaunchedEffect(Unit) {
        if (defaultTab != Route.HOME) navController.navigateTopLevel(defaultTab)
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (showRail) {
            GyNavigationRail(currentRoute, unreadCount) { route ->
                navController.navigateTopLevel(route)
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                // Detail screens (repo, issue, settings) use the full height;
                // the bar slides away instead of eating content space.
                AnimatedVisibility(
                    visible = currentRoute in Route.TOP_LEVEL,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    if (!showRail) {
                        GyBottomBar(currentRoute, unreadCount) { route ->
                            navController.navigateTopLevel(route)
                        }
                    }
                }
            },
        ) { padding ->
            GitYouNavHost(
                navController = navController,
                modifier = Modifier.padding(padding),
                onNotificationsEntered = onRefreshUnread,
            )
        }
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
