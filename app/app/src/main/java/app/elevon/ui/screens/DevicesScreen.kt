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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import app.elevon.input.HostOs
import app.elevon.data.SavedDevice
import app.elevon.hid.HidConnectionState
import app.elevon.ui.components.StateCard
import java.text.DateFormat
import java.util.Date

/**
 * Device management: previously used computers, their status, preferred
 * profile and OS, with friendly names — "My Gaming PC", "Office Laptop".
 */
@Composable
fun DevicesScreen(nav: NavHostController) {
    val session = LocalSession.current
    val devices by session.repos.devices.collectAsState()
    val profiles by session.repos.profiles.collectAsState()
    val connection by session.connection.collectAsState()
    var renaming by remember { mutableStateOf<SavedDevice?>(null) }
    var pickingOs by remember { mutableStateOf<SavedDevice?>(null) }
    var pickingProfile by remember { mutableStateOf<SavedDevice?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        TopAppBar(title = { Text("Devices") })

        if (devices.isEmpty()) {
            StateCard(
                title = "No computers yet",
                body = "On your computer, open Bluetooth settings and look for this phone's name. Pair it like a wireless keyboard — your computer needs nothing installed. It will appear here after the first connection.",
            )
        }

        Spacer(Modifier.height(12.dp))
        devices.forEach { device ->
            val connectedNow = connection.connectionState == HidConnectionState.CONNECTED &&
                connection.hostAddress == device.address
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(8.dp)
                                .background(
                                    if (connectedNow) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(device.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                buildString {
                                    append(device.hostOs.label)
                                    device.lastConnected.takeIf { it > 0 }?.let {
                                        append(" · last used ")
                                        append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)))
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!connectedNow) {
                        OutlinedButton(
                            onClick = { session.connect(device.address, device.name) },
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Connect") }
                    } else {
                        OutlinedButton(
                            onClick = { session.disconnect() },
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Disconnect") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = listOfNotNull(
                            device.preferredProfileId?.let { id -> profiles.firstOrNull { it.id == id }?.name ?: "Profile" },
                        ).joinToString().ifBlank { "No preferred profile" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        IconButton(onClick = { pickingProfile = device }) {
                            Icon(Icons.Outlined.Link, contentDescription = "Choose preferred profile")
                        }
                        IconButton(onClick = { pickingOs = device }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Set computer OS")
                        }
                        IconButton(onClick = { renaming = device }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Rename device")
                        }
                        IconButton(onClick = { session.repos.forgetDevice(device.address) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Forget ${device.name}")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        StateCard(
            title = "Pairing a new computer",
            body = "Make Bluetooth discoverable on this phone from the Home screen's Connect panel, then add the device from your computer's Bluetooth settings. After pairing, use Connect.",
        )
        Spacer(Modifier.height(24.dp))
    }

    renaming?.let { device ->
        var name by remember(device.address) { mutableStateOf(device.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename device") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    session.repos.updateDevice(device.copy(name = name.ifBlank { device.name }))
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }

    pickingOs?.let { device ->
        AlertDialog(
            onDismissRequest = { pickingOs = null },
            title = { Text("Choose the computer type") },
            text = {
                Column {
                    HostOs.entries.forEach { os ->
                        Text(
                            os.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .clickable {
                                    session.repos.updateDevice(device.copy(hostOs = os))
                                    pickingOs = null
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickingOs = null }) { Text("Cancel") } },
        )
    }

    pickingProfile?.let { device ->
        AlertDialog(
            onDismissRequest = { pickingProfile = null },
            title = { Text("Preferred profile for ${device.name}") },
            text = {
                Column {
                    Text(
                        "None",
                        modifier = Modifier
                            .clickable {
                                session.repos.updateDevice(device.copy(preferredProfileId = null))
                                pickingProfile = null
                            }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    profiles.forEach { profile ->
                        Text(
                            profile.name,
                            modifier = Modifier
                                .clickable {
                                    session.repos.updateDevice(device.copy(preferredProfileId = profile.id))
                                    pickingProfile = null
                                }
                                .padding(vertical = 10.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickingProfile = null }) { Text("Cancel") } },
        )
    }
}
