package app.elevon.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.elevon.LocalSession
import app.elevon.ui.screens.AboutScreen
import app.elevon.ui.screens.ClipboardScreen
import app.elevon.ui.screens.CompatScreen
import app.elevon.ui.screens.DevicesScreen
import app.elevon.ui.screens.GamepadScreen
import app.elevon.ui.screens.KeyboardScreen
import app.elevon.ui.screens.LabsScreen
import app.elevon.ui.screens.OnboardingScreen
import app.elevon.ui.screens.ProfilesScreen
import app.elevon.ui.screens.RelayScreen
import app.elevon.ui.screens.RemoteScreens.mediaScreen
import app.elevon.ui.screens.RemoteScreens.presentationScreen
import app.elevon.ui.screens.DeckScreens.deckScreen
import app.elevon.ui.screens.HomeScreen
import app.elevon.ui.screens.SettingsScreen
import app.elevon.ui.screens.TouchpadScreen

/** Top-level destinations that get the bottom bar. */
private data class TopDest(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun AppNav(navController: NavHostController, openRelayDirectly: Boolean) {
    val session = LocalSession.current
    val onboarded by session.settings.onboarded.collectAsState()

    LaunchedEffect(openRelayDirectly) {
        if (openRelayDirectly) navController.navigate("relay")
    }

    val tops = listOf(
        TopDest("home", "Home") { Icon(Icons.Outlined.Home, contentDescription = null) },
        TopDest("devices", "Devices") { Icon(Icons.Outlined.Laptop, contentDescription = null) },
        TopDest("profiles", "Profiles") { Icon(Icons.Outlined.Person, contentDescription = null) },
        TopDest("labs", "Labs") { Icon(Icons.Outlined.DeveloperBoard, contentDescription = null) },
        TopDest("settings", "Settings") { Icon(Icons.Outlined.Settings, contentDescription = null) },
    )
    val currentRoute = navController.currentBackStackEntryAsStateCompat()
    val showBar = tops.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tops.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = dest.icon,
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (onboarded) "home" else "onboarding",
            modifier = Modifier.padding(padding),
        ) {
            composable("onboarding") { OnboardingScreen(onDone = {
                session.settings.setOnboarded()
                navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
            }) }
            composable("home") { HomeScreen(navController) }
            composable("devices") { DevicesScreen(navController) }
            composable("profiles") { ProfilesScreen(navController) }
            composable("labs") { LabsScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
            composable("mode/keyboard") { KeyboardScreen(navController) }
            composable("mode/touchpad") { TouchpadScreen(navController) }
            composable("mode/gamepad") { GamepadScreen(navController) }
            composable("mode/macros") { deckScreen(navController, "macros") }
            composable("mode/custom") { deckScreen(navController, "custom") }
            composable("mode/media") { mediaScreen(navController) }
            composable("mode/presentation") { presentationScreen(navController) }
            composable("relay") { RelayScreen(navController) }
            composable("clipboard") { ClipboardScreen(navController) }
            composable("compat") { CompatScreen(navController) }
            composable("about") { AboutScreen(navController) }
            composable(
                "connect/{address}/{name}",
                arguments = listOf(navArgument("address") {}, navArgument("name") {}),
            ) { entry ->
                // A direct "connect to this device" deep link from Devices list.
                val address = entry.arguments?.getString("address") ?: return@composable
                val name = entry.arguments?.getString("name") ?: "computer"
                androidx.compose.runtime.LaunchedEffect(address) {
                    session.connect(address, name)
                    navController.popBackStack()
                }
            }
        }
    }
}

/** Small helper avoiding the boilerplate of observing the back stack. */
@Composable
private fun NavHostController.currentBackStackEntryAsStateCompat(): String? {
    val entry by currentBackStackEntryFlow
        .collectAsState(initial = currentBackStackEntry)
    return entry?.destination?.route
}
