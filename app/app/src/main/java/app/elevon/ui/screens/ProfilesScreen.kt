package app.elevon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.data.ControlMode
import app.elevon.data.HostLayout
import app.elevon.data.Profile
import app.elevon.input.GamepadProfiles
import app.elevon.input.HostOs
import app.elevon.input.KeyboardLayoutId
import app.elevon.ui.components.StateCard

/**
 * Profiles: one tap restores the right mode, layout, gamepad profile and deck
 * for a given situation or computer.
 */
@Composable
fun ProfilesScreen(nav: NavHostController) {
    val session = LocalSession.current
    val profiles by session.repos.profiles.collectAsState()
    val activeProfile by session.activeProfile.collectAsState()
    var editing by remember { mutableStateOf<Profile?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        TopAppBar(
            title = { Text("Profiles") },
            actions = {
                IconButton(onClick = { creating = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "New profile")
                }
            },
        )
        StateCard(
            title = "What a profile remembers",
            body = "Mode (touchpad, gamepad…), the computer's OS for OS-aware shortcuts, keyboard layout, gamepad profile and macro deck. Profiles can be bound to a computer in Devices.",
        )
        Spacer(Modifier.height(16.dp))

        profiles.forEach { profile ->
            val active = activeProfile?.id == profile.id
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .border(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(18.dp),
                    )
                    .background(
                        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(18.dp),
                    )
                    .clickable { editing = profile }
                    .padding(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${profile.mode.label} · ${profile.hostOs.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        IconButton(onClick = {
                            session.applyProfile(profile)
                            session.setPreferredProfileForCurrentDevice(profile.id)
                        }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Apply ${profile.name}")
                        }
                        IconButton(onClick = { session.repos.deleteProfile(profile.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete ${profile.name}")
                        }
                    }
                }
            }
        }

        Text(
            "Applying a profile sets your default controls. Binding to a device makes it apply automatically when you connect.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }

    if (creating) {
        ProfileEditorDialog(
            existing = null,
            onDismiss = { creating = false },
            onSave = {
                session.repos.upsertProfile(it)
                creating = false
            },
        )
    }
    editing?.let { profile ->
        ProfileEditorDialog(
            existing = profile,
            onDismiss = { editing = null },
            onSave = {
                session.repos.upsertProfile(it)
                editing = null
            },
        )
    }
}

@Composable
private fun ProfileEditorDialog(
    existing: Profile?,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var mode by remember { mutableStateOf(existing?.mode ?: ControlMode.TOUCHPAD) }
    var os by remember { mutableStateOf(existing?.hostOs ?: HostOs.WINDOWS) }
    var kb by remember { mutableStateOf(existing?.keyboardLayout ?: "COMPACT") }
    var pad by remember { mutableStateOf(existing?.gamepadProfileId ?: "std") }
    var deck by remember { mutableStateOf(existing?.macroPageName ?: "Productivity") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New profile" else "Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Text("Opens in", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ControlMode.entries.forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                    }
                }
                Text("Computer OS", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HostOs.entries.forEach { o ->
                        FilterChip(selected = os == o, onClick = { os = o }, label = { Text(o.label) })
                    }
                }
                Text("Keyboard layout", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KeyboardLayoutId.entries.forEach { l ->
                        FilterChip(selected = kb == l.name, onClick = { kb = l.name }, label = { Text(l.label) })
                    }
                }
                Text("Gamepad profile", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GamepadProfiles.defaults().forEach { p ->
                        FilterChip(selected = pad == p.id, onClick = { pad = p.id }, label = { Text(p.name) })
                    }
                }
                OutlinedTextField(value = deck, onValueChange = { deck = it }, label = { Text("Macro deck page") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Profile(
                        id = existing?.id ?: "p_${System.currentTimeMillis()}",
                        name = name.ifBlank { "New profile" },
                        mode = mode,
                        hostOs = os,
                        keyboardLayout = kb,
                        gamepadProfileId = pad,
                        macroPageName = deck,
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
