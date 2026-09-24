package app.elevon.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import app.elevon.LocalSession
import app.elevon.data.AccelerationCurve
import app.elevon.data.GyroMode
import app.elevon.data.HostLayout
import app.elevon.data.HapticsMode
import app.elevon.data.ThemeMode
import app.elevon.ui.components.StateCard

/**
 * Settings: grouped with progressive disclosure. Advanced options live inside
 * sections instead of overwhelming the first screen.
 */
@Composable
fun SettingsScreen(nav: NavHostController) {
    val session = LocalSession.current
    val theme by session.settings.theme.collectAsState()
    val haptics by session.settings.haptics.collectAsState()
    val hostLayoutPref by session.settings.hostLayout.value.collectAsState()
    val keepOn by session.settings.keepScreenOn.value.collectAsState()
    val pointerSpeed by session.settings.pointerSpeed.value.collectAsState()
    val scrollSpeed by session.settings.scrollSpeed.value.collectAsState()
    val natural by session.settings.naturalScrolling.value.collectAsState()
    val tapClick by session.settings.tapToClick.value.collectAsState()
    val dragLock by session.settings.dragLock.value.collectAsState()
    val pointerCurvePref by session.settings.pointerCurve.value.collectAsState()
    val gyroModePref by session.settings.gyroMode.value.collectAsState()
    val gyroSens by session.settings.gyroSensitivity.value.collectAsState()
    val hostLayout = runCatching { HostLayout.valueOf(hostLayoutPref) }.getOrDefault(HostLayout.US)
    val pointerCurve = runCatching { AccelerationCurve.valueOf(pointerCurvePref) }.getOrDefault(AccelerationCurve.LINEAR)
    val gyroMode = runCatching { GyroMode.valueOf(gyroModePref) }.getOrDefault(GyroMode.OFF)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Section("Connection") {
            ToggleRow(
                "Keep screen on while controlling",
                "Prevents the display sleeping mid-session. Costs battery.",
                keepOn,
            ) { session.settings.keepScreenOn.set(it) }
            Text(
                "Reconnection: Elevon remembers paired computers and reconnects from the Devices tab. If the link drops, the app says so — it never leaves you guessing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section("Controls") {
            Text("Pointer speed", style = MaterialTheme.typography.labelLarge)
            Slider(pointerSpeed, { session.settings.pointerSpeed.set(it) }, valueRange = 0.4f..2.6f)
            Text("Scroll speed", style = MaterialTheme.typography.labelLarge)
            Slider(scrollSpeed, { session.settings.scrollSpeed.set(it) }, valueRange = 0.4f..3f)
            Text("Acceleration curve — new in v0.2", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AccelerationCurve.entries.forEach { c ->
                    FilterChip(selected = pointerCurve == c, onClick = { session.settings.pointerCurve.set(c.name) }, label = { Text(c.label) })
                }
            }
            Text(pointerCurve.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ToggleRow("Natural scrolling", "Content follows your fingers.", natural) {
                session.settings.naturalScrolling.set(it)
            }
            ToggleRow("Tap to click", "Quick tap presses the left button.", tapClick) {
                session.settings.tapToClick.set(it)
            }
            ToggleRow("Drag lock", "Hold-to-drag keeps the button down between taps.", dragLock) {
                session.settings.dragLock.set(it)
            }
        }

        Section("Gyro Mouse — Labs") {
            Text("Air mouse via gyroscope — experimental. Tilt phone to move cursor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GyroMode.entries.forEach { m ->
                    FilterChip(selected = gyroMode == m, onClick = { session.settings.gyroMode.set(m.name) }, label = { Text(m.label) })
                }
            }
            Text("Sensitivity", style = MaterialTheme.typography.labelLarge)
            Slider(gyroSens, { session.settings.gyroSensitivity.set(it) }, valueRange = 0.2f..3f)
            Text("Fullscreen landscape supported — ideal for couch gaming.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Section("Keyboard") {
            Text("Host keyboard layout", style = MaterialTheme.typography.labelLarge)
            Text(
                "Set this to match the layout configured on the computer you're controlling, so typed text lands correctly. ${hostLayout.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HostLayout.entries.forEach { layout ->
                    FilterChip(
                        selected = hostLayout == layout,
                        onClick = { session.settings.hostLayout.set(layout.name) },
                        label = { Text(layout.label) },
                    )
                }
            }
            Text(
                "US and UK type accurately. German and French map the base letters; accented characters that need AltGr or dead keys are typed as their closest base character — this is a Bluetooth HID limitation, documented in Compatibility.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section("Appearance") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = theme == mode,
                        onClick = { session.settings.setTheme(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }

        Section("Haptics") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HapticsMode.entries.forEach { mode ->
                    FilterChip(
                        selected = haptics == mode,
                        onClick = { session.settings.setHaptics(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }

        Section("Clipboard") {
            Text(
                "Text you type through Elevon (including macro text and \"send to computer\") is remembered locally for the Clipboard page. Retention can be turned off entirely; clearing removes everything.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { nav.navigate("clipboard") }) { Text("Open Clipboard") }
            }
        }

        Section("Privacy") {
            Text(
                "Elevon has no INTERNET permission — Android physically prevents it from sending data anywhere. Everything stays on this phone: your devices, profiles, layouts, clipboard history and Relay sessions. The website's Privacy page explains exactly what each feature exchanges and why.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section("Labs") {
            Text(
                "Elevon Relay and future experiments live in the Labs tab. They are clearly marked and can change or disappear.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { nav.navigate("labs") }) { Text("Open Labs") }
            }
        }

        Section("About") {
            Text(
                "Elevon 0.1.0 · open source under Apache-2.0. Your phone. Your controls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { nav.navigate("about") }) { Text("About Elevon") }
                TextButton(onClick = { nav.navigate("compat") }) { Text("Compatibility") }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    var open by remember { mutableStateOf(title in setOf("Connection", "Appearance")) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
            .clickable { open = !open }
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Icon(
                if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (open) "Collapse $title" else "Expand $title",
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}
