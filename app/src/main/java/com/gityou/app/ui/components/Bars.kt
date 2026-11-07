package com.gityou.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.gityou.app.R
import com.gityou.app.ui.navigation.Route

private data class NavItem(
    val route: String,
    val label: Int,
    val filled: ImageVector,
    val outlined: ImageVector,
)

private val topNavItems = listOf(
    NavItem(Route.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(Route.SEARCH, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    NavItem(Route.NOTIFICATIONS, R.string.nav_notifications, Icons.Filled.Notifications, Icons.Outlined.Notifications),
    NavItem(Route.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
)

/**
 * The four top-level destinations expressed as NavigationSuiteScaffold items.
 * The suite picks the right container (bar / rail / drawer) per window class;
 * selected icons switch to their filled variant as before.
 */
fun NavigationSuiteScope.gyNavigationSuiteItems(
    currentRoute: String?,
    unreadCount: Int,
    onSelect: (String) -> Unit,
) {
    topNavItems.forEach { navItem ->
        item(
            selected = currentRoute == navItem.route,
            onClick = { onSelect(navItem.route) },
            icon = { NotificationIcon(navItem, currentRoute == navItem.route, unreadCount) },
            label = { Text(stringResource(navItem.label)) },
        )
    }
}

@Composable
private fun NotificationIcon(item: NavItem, selected: Boolean, unreadCount: Int) {
    if (item.route != Route.NOTIFICATIONS) {
        Icon(if (selected) item.filled else item.outlined, contentDescription = null)
        return
    }
    val label = stringResource(R.string.nav_notifications)
    Box {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge { Text(unreadCount.coerceAtMost(99).toString()) }
                }
            },
        ) {
            Icon(
                if (selected) item.filled else item.outlined,
                contentDescription = label,
            )
        }
    }
}
