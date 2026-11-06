package com.gityou.app.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gityou.app.R
import com.gityou.app.ui.components.GyBottomBar
import com.gityou.app.ui.components.GyNavigationRail
import com.gityou.app.ui.navigation.GitYouNavHost
import com.gityou.app.ui.navigation.navigateTopLevel
import com.gityou.app.ui.screens.auth.AuthScreen
import com.gityou.app.ui.theme.GitYouTheme
import com.gityou.app.ui.theme.ThemeMode

private fun themeModeFrom(raw: String): ThemeMode = when (raw) {
    "light" -> ThemeMode.LIGHT
    "dark" -> ThemeMode.DARK
    else -> ThemeMode.SYSTEM
}

@Composable
fun GitYouApp(
    windowSizeClass: WindowWidthSizeClass,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val appState by appViewModel.appState.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val unreadCount by appViewModel.unreadCount.collectAsStateWithLifecycle()

    GitYouTheme(
        themeMode = themeModeFrom(settings.themeMode),
        useDynamicColor = settings.dynamicColor,
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val state = appState) {
                AppViewModel.AppState.Loading -> BrandedLoading()
                AppViewModel.AppState.SignedOut -> AuthScreen()
                is AppViewModel.AppState.SignedIn -> MainScaffold(
                    windowSizeClass = windowSizeClass,
                    unreadCount = unreadCount,
                    onRefreshUnread = appViewModel::refreshUnread,
                )
            }
        }
    }
}

@Composable
private fun MainScaffold(
    windowSizeClass: WindowWidthSizeClass,
    unreadCount: Int,
    onRefreshUnread: () -> Unit,
) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showRail = windowSizeClass == WindowWidthSizeClass.Expanded

    Row(modifier = Modifier.fillMaxSize()) {
        if (showRail) {
            GyNavigationRail(currentRoute, unreadCount) { route ->
                navController.navigateTopLevel(route)
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                if (!showRail) {
                    GyBottomBar(currentRoute, unreadCount) { route ->
                        navController.navigateTopLevel(route)
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
