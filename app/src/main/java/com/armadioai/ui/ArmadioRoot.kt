package com.armadioai.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.armadioai.AppContainer
import com.armadioai.ui.item.ItemDetailScreen
import com.armadioai.ui.outfits.OutfitsScreen
import com.armadioai.ui.settings.SettingsScreen
import com.armadioai.ui.stylist.StylistScreen
import com.armadioai.ui.wardrobe.WardrobeScreen
import kotlinx.coroutines.flow.collectLatest

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    WARDROBE("wardrobe", "Armadio", Icons.Outlined.Checkroom),
    STYLIST("stylist", "Stylist", Icons.Outlined.AutoAwesome),
    OUTFITS("outfits", "Outfit", Icons.Outlined.FavoriteBorder),
    SETTINGS("settings", "Impostazioni", Icons.Outlined.Settings),
}

private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun ArmadioRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Tab.entries.any { it.route == currentRoute }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        container.messages.collectLatest { snackbar.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                snackbar,
                modifier = if (showBottomBar) Modifier else Modifier.navigationBarsPadding(),
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.openTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.WARDROBE.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.WARDROBE.route) {
                WardrobeScreen(
                    onOpenItem = { id -> nav.navigate("item/$id") },
                    onOpenSettings = { nav.openTab(Tab.SETTINGS.route) },
                )
            }
            composable(Tab.STYLIST.route) {
                StylistScreen(onOpenSettings = { nav.openTab(Tab.SETTINGS.route) })
            }
            composable(Tab.OUTFITS.route) { OutfitsScreen() }
            composable(Tab.SETTINGS.route) { SettingsScreen() }
            composable(
                route = "item/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) {
                ItemDetailScreen(
                    onBack = { nav.popBackStack() },
                    onCreateOutfit = {
                        nav.popBackStack()
                        nav.openTab(Tab.STYLIST.route)
                    },
                )
            }
        }
    }
}
