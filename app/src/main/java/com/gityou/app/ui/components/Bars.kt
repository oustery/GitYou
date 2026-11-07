package com.gityou.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
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

@Composable
fun GyBottomBar(
    currentRoute: String?,
    unreadCount: Int,
    onSelect: (String) -> Unit,
) {
    NavigationBar {
        topNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onSelect(item.route) },
                icon = {
                    NotificationIcon(item, currentRoute == item.route, unreadCount)
                },
                label = { Text(stringResource(item.label)) },
            )
        }
    }
}

@Composable
fun GyNavigationRail(
    currentRoute: String?,
    unreadCount: Int,
    onSelect: (String) -> Unit,
) {
    NavigationRail {
        Spacer(Modifier.weight(1f))
        topNavItems.forEach { item ->
            NavigationRailItem(
                selected = currentRoute == item.route,
                onClick = { onSelect(item.route) },
                icon = { NotificationIcon(item, currentRoute == item.route, unreadCount) },
                label = { Text(stringResource(item.label)) },
            )
        }
        Spacer(Modifier.weight(1f))
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
