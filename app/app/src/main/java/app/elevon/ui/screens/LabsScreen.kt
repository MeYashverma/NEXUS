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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
 * NEXUS Labs → now Elevon Labs: a visible home for experimental features.
 * Planned experiments are listed honestly as "planned" — tapping them opens
 * an explanation, never a fake feature.
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
            "Experiments. They may change, break, or disappear — and they never require installing anything on your computer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        // ---- Relay (live experiment) ----
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

        Spacer(Modifier.height(14.dp))

        // ---- planned experiments (honest placeholders with real explanations) ----
        PlannedCard("Wi-Fi Relay", onOpen = { plannedInfo = WIFI_RELAY_NOTE })
        PlannedCard("Deck cloud sync", onOpen = { plannedInfo = DECK_SYNC_NOTE })
        PlannedCard("Pointer acceleration curves", onOpen = { plannedInfo = CURVE_NOTE })

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
        "A Wi-Fi variant would work from any browser, but a web page served by the phone is not a secure context, so it cannot use Web Crypto, and it would require granting Elevon the INTERNET permission — which we refuse to do for now. Re-evaluating."

private const val DECK_SYNC_NOTE =
    "Sharing decks between phones needs storage somewhere. Any sync would be opt-in and end-to-end encrypted, or file-export based. Not started."

private const val CURVE_NOTE =
    "Fine-grained mouse curve tuning (like driver software, but without the driver). Exploring what's expressive enough in plain HID reports."

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
