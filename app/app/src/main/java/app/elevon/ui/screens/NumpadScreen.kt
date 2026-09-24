package app.elevon.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.hid.HidConnectionState
import app.elevon.hid.Keycodes
import app.elevon.ui.components.StateCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Numpad mode — numeric keypad with calculations.
 * Fullscreen landscape support: tap fullscreen → landscape, immersive, keys fill screen.
 * Useful for spreadsheets, calculators, data entry.
 */
@Composable
fun NumpadScreen(nav: NavHostController) {
    val session = LocalSession.current
    val connection by session.connection.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isSystemLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isFullscreen by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val connected = connection.connectionState == HidConnectionState.CONNECTED
    val useFullscreen = isFullscreen || isSystemLandscape

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun tap(usage: Int) {
        if (!connected) return
        session.hid.sendRawKeyboard(0, listOf(usage))
        scope.launch {
            delay(12)
            session.hid.sendRawKeyboard(0, emptyList())
        }
    }

    fun tapWithMod(mod: Int, usage: Int) {
        if (!connected) return
        session.hid.sendRawKeyboard(mod, listOf(usage))
        scope.launch {
            delay(12)
            session.hid.sendRawKeyboard(0, emptyList())
        }
    }

    @Composable
    fun NumpadGrid(fullscreen: Boolean) {
        val keyHeight = if (fullscreen) Modifier.weight(1f) else Modifier.height(64.dp)
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row: 7 8 9 /
            Row(Modifier.fillMaxWidth().then(if (fullscreen) Modifier.weight(1f) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumpadKey("7", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_7) }
                NumpadKey("8", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_8) }
                NumpadKey("9", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_9) }
                NumpadKey("/", Modifier.weight(1f).then(keyHeight), fullscreen, isOp = true) { tap(Keycodes.KEY_KP_SLASH) }
            }
            Row(Modifier.fillMaxWidth().then(if (fullscreen) Modifier.weight(1f) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumpadKey("4", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_4) }
                NumpadKey("5", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_5) }
                NumpadKey("6", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_6) }
                NumpadKey("*", Modifier.weight(1f).then(keyHeight), fullscreen, isOp = true) { tap(Keycodes.KEY_KP_ASTERISK) }
            }
            Row(Modifier.fillMaxWidth().then(if (fullscreen) Modifier.weight(1f) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumpadKey("1", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_1) }
                NumpadKey("2", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_2) }
                NumpadKey("3", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_3) }
                NumpadKey("-", Modifier.weight(1f).then(keyHeight), fullscreen, isOp = true) { tap(Keycodes.KEY_KP_MINUS) }
            }
            Row(Modifier.fillMaxWidth().then(if (fullscreen) Modifier.weight(1f) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumpadKey("0", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_0) }
                NumpadKey(".", Modifier.weight(1f).then(keyHeight), fullscreen) { tap(Keycodes.KEY_KP_DOT) }
                NumpadKey("=", Modifier.weight(1f).then(keyHeight), fullscreen, isOp = true) { tap(Keycodes.KEY_KP_EQUAL) }
                NumpadKey("+", Modifier.weight(1f).then(keyHeight), fullscreen, isOp = true) { tap(Keycodes.KEY_KP_PLUS) }
            }
            Row(Modifier.fillMaxWidth().then(if (fullscreen) Modifier.weight(1f) else Modifier), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumpadKey("Esc", Modifier.weight(1f).then(keyHeight), fullscreen, isOp = true) { tap(Keycodes.KEY_ESC) }
                NumpadKey("Tab", Modifier.weight(1f).then(keyHeight), fullscreen, isOp = true) { tap(Keycodes.KEY_TAB) }
                NumpadKey("⌫", Modifier.weight(1f).then(keyHeight), fullscreen, isOp = true) { tap(Keycodes.KEY_BACKSPACE) }
                NumpadKey("Enter", Modifier.weight(1f).then(keyHeight), fullscreen, isPrimary = true) { tap(Keycodes.KEY_KP_ENTER) }
            }
        }
    }

    if (useFullscreen) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (isFullscreen) isFullscreen = false else nav.popBackStack() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("Numpad · Fullscreen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) { Icon(if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen, contentDescription = "Toggle fullscreen") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (!connected) {
                    StateCard(title = "No computer connected", body = "Connect from Home to send numpad keys. Fullscreen landscape for spreadsheets.")
                    Spacer(Modifier.height(12.dp))
                }
                Box(Modifier.weight(1f)) { NumpadGrid(fullscreen = true) }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Numpad") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { isFullscreen = true }) { Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen landscape") }
                    IconButton(onClick = { activity?.requestedOrientation = if (isSystemLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }) {
                        Icon(Icons.Outlined.ScreenRotation, contentDescription = "Rotate")
                    }
                }
            )
            if (!connected) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    StateCard(title = "No computer connected", body = "Numpad sends keypad keys — great for Excel, calculators. Connect from Home. Fullscreen for landscape data entry.")
                    Spacer(Modifier.height(8.dp))
                }
            }
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                NumpadGrid(fullscreen = false)
                Spacer(Modifier.height(12.dp))
                Text("Tip: Tap fullscreen for landscape spreadsheet work. NumLock is handled by host.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NumpadKey(label: String, modifier: Modifier, fullscreen: Boolean, isOp: Boolean = false, isPrimary: Boolean = false, onTap: () -> Unit) {
    Box(
        modifier = modifier
            .border(1.dp, if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(if (fullscreen) 16.dp else 14.dp))
            .background(
                when {
                    isPrimary -> MaterialTheme.colorScheme.primary
                    isOp -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                RoundedCornerShape(if (fullscreen) 16.dp else 14.dp)
            )
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = if (fullscreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
