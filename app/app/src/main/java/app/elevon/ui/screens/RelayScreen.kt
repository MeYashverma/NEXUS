package app.elevon.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.hid.ServiceLocator
import app.elevon.relay.RelayState
import app.elevon.relay.RelayUi
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard

/**
 * Elevon Relay: the laptop keyboard types on this phone through the browser
 * (elevon.app/relay). End-to-end encrypted, with a 5-digit comparison code.
 * A RELAY ACTIVE banner is always visible while it runs, and STOP is huge.
 */
@Composable
fun RelayScreen(nav: NavHostController) {
    val session = LocalSession.current
    val context = LocalContext.current
    val relay = remember { ServiceLocator.relayManager(context) }
    val ui by relay.bus.ui.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            if (ui.state != RelayState.ACTIVE && ui.state != RelayState.HANDSHAKE) {
                // Leaving the screen with Relay idle: stop advertising quietly.
                if (ui.state == RelayState.WAITING) relay.stop()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        TopAppBar(
            title = { Text("Relay") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            actions = { HonestyChip(Honesty.EXPERIMENTAL, Modifier.padding(end = 12.dp)) },
        )

        // ---- state panel ----
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (ui.state) {
                RelayState.OFF, RelayState.STOPPED -> {
                    Text("Relay is off", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Start Relay, then open elevon.app/relay in your laptop's browser and pick this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { relay.start() }, shape = RoundedCornerShape(14.dp)) {
                        Text("Start Relay")
                    }
                }

                RelayState.WAITING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.padding(start = 10.dp))
                        Text("Waiting for your laptop…", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "On your laptop, open the Relay page and choose this phone from the list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(onClick = { relay.stop() }, shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel")
                    }
                }

                RelayState.HANDSHAKE -> {
                    Text("Connecting", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Compare this code with your laptop", style = MaterialTheme.typography.bodySmall)
                    Text(
                        ui.code ?: "·····",
                        fontSize = 44.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                RelayState.ACTIVE -> {
                    // The visible safety banner: RELAY ACTIVE + who is connected.
                    Text("RELAY ACTIVE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("Connected to: ${ui.laptopName ?: "your laptop"}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Code ${ui.code ?: "·····"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (ui.buffer.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Received: …${ui.buffer.takeLast(80)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { relay.stopAndClear() },
                        shape = RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.height(56.dp),
                    ) {
                        Text("STOP RELAY", style = MaterialTheme.typography.titleMedium)
                    }
                }

                RelayState.ERROR -> {
                    Text("Relay can't start here", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (ui.error) {
                            app.elevon.relay.RelayManager.ERROR_BLUETOOTH_OFF ->
                                "Bluetooth is off. Turn it on and try again."
                            app.elevon.relay.RelayManager.ERROR_NEEDS_PERMISSION ->
                                "Relay needs the Nearby devices permission to advertise itself to your laptop."
                            app.elevon.relay.RelayManager.ERROR_NO_ADVERTISER,
                            app.elevon.relay.RelayManager.ERROR_ADVERTISE_FAILED ->
                                "This phone can't advertise over Bluetooth LE right now. Some phones restrict it; try toggling Bluetooth or restarting the phone.",
                            app.elevon.relay.RelayManager.ERROR_HANDSHAKE ->
                                "The secure handshake failed. Stop, start again and re-compare the code.",
                            else -> "Something didn't work. Stop and start again."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            if (ui.error == app.elevon.relay.RelayManager.ERROR_NEEDS_PERMISSION) {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
                            } else {
                                relay.start()
                            }
                        }, shape = RoundedCornerShape(14.dp)) { Text("Try again") }
                        OutlinedButton(onClick = { nav.navigate("compat") }, shape = RoundedCornerShape(14.dp)) {
                            Text("Troubleshoot")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- what this is / is not ----
        StateCard(
            title = "Where typed text goes",
            body = "With the Elevon Relay keyboard enabled, text lands in whatever field is focused — messages, search bars, URLs. Without the IME, Relay still collects text into a buffer here that you can copy. The session is end-to-end encrypted; the 5-digit code proves you're talking to your own laptop.",
        )
        Spacer(Modifier.height(10.dp))
        StateCard(
            title = "What Relay cannot do",
            body = "A web page only sees keys typed while the Relay page is focused. It cannot capture your laptop's keyboard system-wide — no website can. Switching apps on the laptop pauses Relay; you resume it deliberately. That is the browser's security model, and Elevon won't pretend around it.",
        )
        Spacer(Modifier.height(10.dp))
        StateCard(
            title = "Set up the Relay keyboard",
            body = "Settings → System → Keyboards → On-screen keyboard → Manage keyboards → enable “Elevon Relay”. It only ever inserts text that your own Relay session sends, and the banner above is always visible while it does.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }) { Text("Open keyboard settings") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
