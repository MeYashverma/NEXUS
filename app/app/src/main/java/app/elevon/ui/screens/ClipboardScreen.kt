package app.elevon.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.ui.components.StateCard

/**
 * Clipboard bridge. Honest about the two directions:
 *  - Phone → computer: Elevon types the text as keystrokes (works everywhere,
 *    because it's just the keyboard).
 *  - Computer → phone: only through Elevon Relay's paste box — a normal web
 *    page cannot watch the computer's clipboard without host software.
 */
@Composable
fun ClipboardScreen(nav: NavHostController) {
    val session = LocalSession.current
    val history by session.settings.clipboardHistory.collectAsState()
    val retention by session.settings.clipboardRetention.collectAsState()
    val connection by session.connection.collectAsState()
    val clipboard = LocalClipboardManager.current
    val connected = connection.connectionState == app.elevon.hid.HidConnectionState.CONNECTED

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        TopAppBar(
            title = { Text("Clipboard") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            StateCard(
                title = "How clipboard sharing works here",
                body = "Phone → computer: Elevon types the text out as keystrokes, so it lands wherever your cursor is. Computer → phone: paste into the Elevon Relay page in your laptop's browser (Labs). Nothing watches the computer's clipboard in the background — that would require host software, which Elevon refuses to need.",
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Remember shared text", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Keeps the last 20 items on this phone only. Turn off to keep nothing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = retention,
                    onCheckedChange = {
                        session.settings.clipboardRetention.set(it)
                        if (!it) session.settings.clearClipboardHistory()
                    },
                )
            }
            if (history.isNotEmpty()) {
                TextButton(onClick = { session.settings.clearClipboardHistory() }) {
                    Text("Delete all history")
                }
            }
            Spacer(Modifier.height(8.dp))

            if (history.isEmpty()) {
                StateCard(
                    title = "Nothing shared yet",
                    body = "Typed text, macro snippets and Relay paste-ins appear here so you can copy them again later.",
                )
            } else {
                history.forEachIndexed { index, text ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { clipboard.setText(AnnotatedString(text)) }) {
                                Text("Copy to phone")
                            }
                            TextButton(onClick = {
                                session.sendClipboardToComputer(text, {}, {})
                            }) {
                                Text("Type on computer")
                            }
                            TextButton(onClick = {
                                val next = history.toMutableList().apply { removeAt(index) }
                                session.settings.clearClipboardHistory()
                                next.forEach { reversed -> Unit }
                                // Re-add remaining items in original order.
                                next.reversed().forEach { item ->
                                    // rememberClipboard prepends; rebuild silently.
                                }
                                session.settings.setClipboardHistory(next)
                            }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}
