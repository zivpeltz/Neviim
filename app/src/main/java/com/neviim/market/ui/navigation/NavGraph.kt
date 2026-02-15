package com.neviim.market.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.neviim.market.R
import com.neviim.market.ui.screen.*

// ── Route constants ─────────────────────────────────────────────────
object Routes {
    const val EXPLORE = "explore"
    const val EVENT_DETAIL = "event_detail/{eventId}"
    const val PORTFOLIO = "portfolio"
    const val ACCOUNT = "account"

    fun eventDetail(eventId: String) = "event_detail/$eventId"
}

data class BottomNavItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Routes.EXPLORE,
        labelResId = R.string.nav_explore,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    ),
    BottomNavItem(
        route = Routes.PORTFOLIO,
        labelResId = R.string.nav_my_bids,
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    ),
    BottomNavItem(
        route = Routes.ACCOUNT,
        labelResId = R.string.nav_account,
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle
    )
)

@Composable
fun NeviimNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Hide bottom bar on detail screens
            val showBottomBar = bottomNavItems.any { item ->
                currentDestination?.hierarchy?.any { it.route == item.route } == true
            }

            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelResId)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelResId),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.EXPLORE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onEventClick = { eventId ->
                        navController.navigate(Routes.eventDetail(eventId))
                    }
                )
            }

            composable(
                Routes.EVENT_DETAIL,
                arguments = listOf(
                    navArgument("eventId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                EventDetailScreen(
                    eventId = eventId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PORTFOLIO) {
                PortfolioScreen()
            }

            composable(Routes.ACCOUNT) {
                AccountScreen()
            }
        }
    }
}
