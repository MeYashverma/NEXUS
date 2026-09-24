package app.elevon.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.data.ControlMode
import app.elevon.hid.HidConnectionState
import app.elevon.ui.components.ConnectionPill
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard

/**
 * The home screen: brand, live connection state, the seven control modes,
 * profiles, quick actions and a Labs teaser. Design per docs/design.md §22.
 */
@Composable
fun HomeScreen(nav: NavHostController) {
    val session = LocalSession.current
    val connection by session.connection.collectAsState()
    val profiles by session.repos.profiles.collectAsState()
    val activeProfile by session.activeProfile.collectAsState()
    val macroRun by session.macroRun.collectAsState()
    var showConnect by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Wordmark: glyph + name, drawn from the same vector family as the icon.
                Box(
                    Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("E", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Text("Elevon", style = MaterialTheme.typography.titleLarge)
            }
            ConnectionPill(connection)
        }

        Spacer(Modifier.height(20.dp))

        // ---- connection card ----
        ConnectionCard(
            connectionState = connection.connectionState,
            hostName = connection.hostName,
            onConnect = { showConnect = !showConnect },
            onDisconnect = { session.disconnect() },
        )

        AnimatedVisibility(visible = showConnect) {
            ConnectSheet(nav)
        }

        Spacer(Modifier.height(20.dp))

        // ---- profiles strip ----
        if (profiles.isNotEmpty()) {
            Text("Profiles", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .androidx.compose.foundation.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                profiles.take(4).forEach { profile ->
                    val selected = activeProfile?.id == profile.id
                    OutlinedButton(
                        onClick = {
                            session.applyProfile(profile)
                            session.setPreferredProfileForCurrentDevice(profile.id)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Text(profile.name, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ---- mode grid ----
        Text("Controls", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val modes = listOf(
            ModeEntry(ControlMode.KEYBOARD, Icons.Outlined.Keyboard, Honesty.CORE),
            ModeEntry(ControlMode.TOUCHPAD, Icons.Outlined.TouchApp, Honesty.CORE),
            ModeEntry(ControlMode.GAMEPAD, Icons.Outlined.SportsEsports, Honesty.CORE),
            ModeEntry(ControlMode.MACROS, Icons.Outlined.ViewModule, Honesty.CORE),
            ModeEntry(ControlMode.MEDIA, Icons.Outlined.Pause, Honesty.CORE),
            ModeEntry(ControlMode.PRESENTATION, Icons.Outlined.Slideshow, Honesty.CORE),
            ModeEntry(ControlMode.CUSTOM, Icons.Outlined.Gamepad, Honesty.CORE),
        )
        modes.chunked(2).forEach { rowModes ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowModes.forEach { entry ->
                    ModeCard(
                        entry = entry,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.7f)
                            .semantics { contentDescription = "Open ${entry.mode.label} mode" },
                        onClick = { nav.navigate("mode/${entry.mode.name.lowercase()}") },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // ---- labs teaser ----
        Spacer(Modifier.height(6.dp))
        StateCard(
            title = "Elevon Labs",
            body = "Relay — use your laptop keyboard to type on your phone. Experimental, browser-based, end-to-end encrypted.",
            modifier = Modifier.clickable { nav.navigate("relay") },
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StateCard(
                title = "Will this work on my phone?",
                body = "Check compatibility and what each mode honestly supports.",
                modifier = Modifier
                    .weight(1f)
                    .clickable { nav.navigate("compat") },
            )
            StateCard(
                title = "Clipboard",
                body = "Recently shared text between your phone and computer.",
                modifier = Modifier
                    .weight(1f)
                    .clickable { nav.navigate("clipboard") },
            )
        }

        Spacer(Modifier.height(16.dp))
        if (macroRun != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Running “${macroRun!!.label}” (${macroRun!!.step}/${macroRun!!.totalSteps})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ConnectionCard(
    connectionState: HidConnectionState,
    hostName: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        when (connectionState) {
            HidConnectionState.CONNECTED -> {
                Text("Connected to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(hostName ?: "computer", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onDisconnect, shape = RoundedCornerShape(12.dp)) {
                        Text("Disconnect")
                    }
                }
            }
            HidConnectionState.CONNECTING -> {
                Text("Connecting", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Your computer may show a pairing prompt — accept it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Waiting for ${hostName ?: "computer"}…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HidConnectionState.UNSUPPORTED -> {
                Text("This phone can't be a Bluetooth input device", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Text(
                    "Some phone makers switch off the Bluetooth HID profile Elevon needs. Nothing is wrong with your computer. You can still explore the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HidConnectionState.OFFLINE -> {
                Text("Bluetooth is off", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Turn Bluetooth on to connect to a computer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val context = LocalContext.current
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }, shape = RoundedCornerShape(12.dp)) { Text("Open Bluetooth settings") }
            }
            else -> {
                Text("No computer connected", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Pair once, like any wireless keyboard. Your computer needs nothing installed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onConnect, shape = RoundedCornerShape(12.dp)) {
                    Text(if (connectionState == HidConnectionState.DISCONNECTED) "Reconnect" else "Connect")
                }
            }
        }
    }
}

/** Expandable list of paired computers plus first-time pairing guidance. */
@Composable
private fun ConnectSheet(nav: NavHostController) {
    val session = LocalSession.current
    val hosts = remember { session.hid.pairedHosts() }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text("Your computers", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (hosts.isEmpty()) {
            Text(
                "No computers paired yet. On your computer, look for a device named after this phone in its Bluetooth settings and pair. Then it appears here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            hosts.forEach { host ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { session.connect(host.address, host.name) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Laptop, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(host.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text("Connect", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pair a new computer")
        }
    }
}

@Composable
private fun ModeCard(entry: ModeEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            HonestyChip(entry.honesty)
        }
        Column {
            Text(entry.mode.label, style = MaterialTheme.typography.titleMedium)
            Text(
                entry.mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private data class ModeEntry(val mode: ControlMode, val icon: ImageVector, val honesty: Honesty)

private fun Modifier.horizontalScrollRow(): Modifier = this.then(
    Modifier,
)
