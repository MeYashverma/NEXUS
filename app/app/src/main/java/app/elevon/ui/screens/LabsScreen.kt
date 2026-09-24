package app.elevon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard

/**
 * Elevon Labs: experimental features, now with real implementations.
 * - Relay (live)
 * - Gyro Mouse (new, air mouse via gyroscope, fullscreen landscape)
 * - Numpad (new, numeric keypad, fullscreen)
 * - Pointer curves (new, acceleration curves for touchpad)
 * - Deck export/import (new, file-based, no cloud)
 * - Wi-Fi Relay (planned, with honest trade-off note)
 */
@Composable
fun LabsScreen(nav: NavHostController) {
    var plannedInfo by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        TopAppBar(
            title = { Text("Elevon Labs") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
        )
        Text(
            "Experiments that may change, break, or disappear — but never require host software for core. New: gyro mouse, numpad, pointer curves, deck export/import.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        // Relay (live experiment)
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                .clickable { nav.navigate("relay") }
                .padding(20.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Relay", style = MaterialTheme.typography.titleLarge)
                HonestyChip(Honesty.EXPERIMENTAL)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Use the keyboard already in front of you to type on your phone. Your laptop's browser talks to Elevon over Bluetooth — nothing installed, end-to-end encrypted, always one tap from STOP.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Open Relay",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // New Labs features — real
        LabFeatureCard(
            title = "Gyro Mouse",
            subtitle = "Air mouse via gyroscope — tilt to move, tap to click. Fullscreen landscape for couch.",
            icon = { Icon(Icons.Outlined.Sensors, contentDescription = null) },
            honesty = Honesty.EXPERIMENTAL,
            onOpen = { nav.navigate("mode/gyro") }
        )
        LabFeatureCard(
            title = "Numpad",
            subtitle = "Numeric keypad with calculations. Fullscreen landscape for spreadsheets and data entry.",
            icon = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
            honesty = Honesty.CORE,
            onOpen = { nav.navigate("mode/numpad") }
        )
        LabFeatureCard(
            title = "Pointer acceleration curves",
            subtitle = "Linear, ease-out, precise, gaming — choose how touchpad and gyro feel. In Settings → Controls.",
            icon = { Icon(Icons.Outlined.Speed, contentDescription = null) },
            honesty = Honesty.CORE,
            onOpen = { nav.navigate("settings") }
        )
        LabFeatureCard(
            title = "Deck export / import",
            subtitle = "Export macro decks to JSON, share, import. File-based, no cloud. In Macro Pad → Export.",
            icon = { Icon(Icons.Outlined.Upload, contentDescription = null) },
            honesty = Honesty.CORE,
            onOpen = { nav.navigate("mode/macros") }
        )

        Spacer(Modifier.height(12.dp))
        Text("Planned — honest status", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        PlannedCard("Wi-Fi Relay", onOpen = { plannedInfo = WIFI_RELAY_NOTE })
        PlannedCard("Deck cloud sync", onOpen = { plannedInfo = DECK_SYNC_NOTE })
        PlannedCard("Laser pointer", onOpen = { plannedInfo = LASER_NOTE })

        Spacer(Modifier.height(24.dp))
        StateCard(
            title = "Why Labs exists",
            body = "Core must work with no host software. Labs is where we try things that need new permissions or new paradigms — and we label the trade-offs. Gyro Mouse needs gyroscope, Numpad is pure HID, Pointer curves are just math, Deck export is file-based. Wi-Fi Relay would need INTERNET permission — we refuse to add it silently."
        )
        Spacer(Modifier.height(24.dp))
    }

    plannedInfo?.let { text ->
        AlertDialog(
            onDismissRequest = { plannedInfo = null },
            title = { Text("Planned — not built yet") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { plannedInfo = null }) { Text("Got it") }
            },
        )
    }
}

private const val WIFI_RELAY_NOTE =
    "The browser Relay only works where Web Bluetooth exists (Chrome/Edge on Windows, macOS and ChromeOS). " +
        "A Wi-Fi variant would work from any browser, but a web page served by the phone is not a secure context, so it cannot use Web Crypto, and it would require granting Elevon the INTERNET permission — which we refuse to do for now. We have a prototype that uses local HTTP + NSD, but it needs INTERNET. Re-evaluating with clear opt-in."

private const val DECK_SYNC_NOTE =
    "Sharing decks between phones needs storage somewhere. Current solution is file export/import (JSON) — you can share via any messenger, no cloud. Encrypted cloud sync would be opt-in and E2E. Not started."

private const val LASER_NOTE =
    "Laser pointer for presentations: uses gyro + presentation mode to show a dot on slides. Needs camera permission? No — just gyro + arrow keys. Prototype works but needs calibration. Planned for next Labs drop."

private const val CURVE_NOTE =
    "Fine-grained mouse curve tuning (like driver software, but without the driver). Now shipped: Settings → Pointer curve with Linear, Ease-out, Precise, Gaming."

@Composable
private fun LabFeatureCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    honesty: Honesty,
    onOpen: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icon()
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            HonestyChip(honesty)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Open", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp).padding(top = 2.dp),)
        }
    }
}

@Composable
private fun PlannedCard(title: String, onOpen: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HonestyChip(Honesty.LIMITED)
        }
        Text(
            "Tap for the honest status",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
