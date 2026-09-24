package app.elevon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.data.ControlMode
import app.elevon.data.HostOs
import app.elevon.hid.HidConnectionState
import app.elevon.hid.Keycodes
import app.elevon.input.Chord
import app.elevon.input.MacroAction
import app.elevon.input.MacroPage
import app.elevon.input.MacroPresets
import app.elevon.input.MacroStep
import app.elevon.input.OsShortcuts
import app.elevon.input.ShortcutId
import app.elevon.input.describe
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard

/**
 * Macro Pad and Custom surfaces share this deck engine: pages of 12 big
 * buttons, each running honest keyboard/media/text actions — no desktop
 * server needed, unlike Stream Deck Mobile or Macro Deck.
 */
object DeckScreens {

    @Composable
    fun deckScreen(nav: NavHostController, kind: String) {
        val session = LocalSession.current
        val connection by session.connection.collectAsState()
        val macroRun by session.macroRun.collectAsState()
        val userDecks by session.repos.decks.collectAsState()
        val hostOs = session.hostOs()

        val presetPages = remember(hostOs, kind) {
            if (kind == "macros") {
                listOf(
                    MacroPage("d_productivity", "Productivity", MacroPresets.productivity(hostOs)),
                    MacroPage("d_media", "Media", MacroPresets.media()),
                    MacroPage("d_snippets", "Snippets", MacroPresets.snippets()),
                )
            } else {
                deckStarterPages(hostOs)
            }
        }
        val pages = remember(presetPages, userDecks) { presetPages + userDecks }

        var pageIndex by remember { mutableStateOf(0) }
        LaunchedEffect(pages.size) { if (pageIndex >= pages.size) pageIndex = 0 }

        var editingSlot by remember { mutableStateOf<Pair<String, Int>?>(null) } // (pageId, slot)
        var renamingPage by remember { mutableStateOf<String?>(null) }

        val page = pages.getOrNull(pageIndex)

        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(if (kind == "macros") "Macro Pad" else "Custom surface") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { HonestyChip(Honesty.CORE, Modifier.padding(end = 12.dp)) },
            )

            if (connection.connectionState != HidConnectionState.CONNECTED) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    StateCard(
                        title = "No computer connected",
                        body = "Buttons light up but only send while connected. Connect from Home.",
                    )
                }
            }

            // Page tabs
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pages.forEachIndexed { index, p ->
                    FilterChip(
                        selected = index == pageIndex,
                        onClick = { pageIndex = index },
                        label = {
                            Text(
                                p.name,
                                modifier = Modifier.clickable { },
                            )
                        },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = {
                        val id = "u_${System.currentTimeMillis()}"
                        session.repos.upsertDeck(MacroPage(id, "New page", List(12) { null }))
                    },
                    label = { Text("Add page") },
                )
            }

            if (macroRun != null) {
                Text(
                    "Running “${macroRun!!.label}” (${macroRun!!.step}/${macroRun!!.totalSteps}) — tap Cancel to stop",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { session.cancelMacro() }
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            // The deck grid
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                page?.buttons?.chunked(3)?.forEach { rowButtons ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowButtons.forEachIndexed { col, action ->
                            val slotIndex = (page.buttons.indexOf(action)).let { idx ->
                                // index within row chunk + row offset
                                col + page.buttons.chunked(3).indexOfFirst { it === rowButtons } * 3
                            }
                            DeckButton(
                                action = action,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.45f),
                                onTap = {
                                    if (action != null) session.runMacro(action.label, action.steps)
                                },
                                onLongPress = { editingSlot = page.id to slotIndex },
                            )
                        }
                    }
                }
                Text(
                    "Tap to run · long-press to edit. Actions use standard keyboard and media keys, so they work with no software on the computer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // Slot editor dialog
        editingSlot?.let { (pageId, slot) ->
            val target = pages.firstOrNull { it.id == pageId }
            if (target != null) {
                SlotEditorDialog(
                    existing = target.buttons.getOrNull(slot),
                    onDismiss = { editingSlot = null },
                    onSave = { action ->
                        val buttons = target.buttons.toMutableList()
                        while (buttons.size < MacroPage.SLOTS) buttons.add(null)
                        buttons[slot] = action
                        session.repos.upsertDeck(target.copy(buttons = buttons))
                        editingSlot = null
                    },
                    onClear = {
                        val buttons = target.buttons.toMutableList()
                        if (slot < buttons.size) buttons[slot] = null
                        session.repos.upsertDeck(target.copy(buttons = buttons))
                        editingSlot = null
                    },
                )
            }
        }
    }

    /** Starter decks for the Custom surface, honest about being keyboard-driven. */
    private fun deckStarterPages(hostOs: HostOs): List<MacroPage> = listOf(
        MacroPage("c_streaming", "Streaming", streamingDeck(hostOs)),
        MacroPage("c_editing", "Editing", editingDeck(hostOs)),
        MacroPage("c_presenting", "Presenting", MacroPresets.productivity(hostOs).mapIndexed { i, a ->
            when (i) {
                0 -> MacroAction("blank", "Blank", "b key", listOf(MacroStep.Chord(0, Keycodes.KEY_A + ('b' - 'a'))))
                1 -> MacroAction("resume", "Resume", "b key", listOf(MacroStep.Chord(0, Keycodes.KEY_A + ('b' - 'a'))))
                else -> a
            }
        }),
        MacroPage("c_music", "Music", MacroPresets.media()),
    )

    private fun streamingDeck(hostOs: HostOs): List<MacroAction?> {
        val s = OsShortcuts.forOs(hostOs)
        fun act(id: String, label: String, sub: String, chord: Chord) =
            MacroAction(id, label, sub, listOf(MacroStep.Chord(chord.mods, chord.usage)))
        return listOf(
            MacroAction("ptt", "Push to talk", "Hold button = hold V", listOf(MacroStep.Chord(0, app.elevon.hid.Keycodes.KEY_A + ('v' - 'a')))),
            MacroAction("mute", "Mute mic", "Set your app hotkey", act("mute2", "Mute", "M", Chord(0, app.elevon.hid.Keycodes.KEY_A + ('m' - 'a')))),
            act("screenshot", "Screenshot", s[ShortcutId.SCREENSHOT]!!.describe(), s[ShortcutId.SCREENSHOT]!!),
            act("switchapp", "Switch app", s[ShortcutId.SWITCH_APP]!!.describe(), s[ShortcutId.SWITCH_APP]!!),
            MacroAction("next", "Next track", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_SCAN_NEXT))),
            MacroAction("mute2", "Mute audio", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_MUTE))),
            act("showdesktop", "Show desktop", s[ShortcutId.SHOW_DESKTOP]!!.describe(), s[ShortcutId.SHOW_DESKTOP]!!),
            act("copy", "Copy", s[ShortcutId.COPY]!!.describe(), s[ShortcutId.COPY]!!),
            act("paste", "Paste", s[ShortcutId.PASTE]!!.describe(), s[ShortcutId.PASTE]!!),
            null, null, null,
        )
    }

    private fun editingDeck(hostOs: HostOs): List<MacroAction?> {
        val s = OsShortcuts.forOs(hostOs)
        fun act(id: ShortcutId) = MacroAction(
            id.name.lowercase(), id.label, s[id]!!.describe(),
            listOf(MacroStep.Chord(s[id]!!.mods, s[id]!!.usage)),
        )
        return listOf(
            act(ShortcutId.UNDO),
            act(ShortcutId.REDO),
            act(ShortcutId.CUT),
            act(ShortcutId.COPY),
            act(ShortcutId.PASTE),
            act(ShortcutId.SAVE),
            act(ShortcutId.FIND),
            act(ShortcutId.SELECT_ALL),
            act(ShortcutId.NEW_TAB),
            act(ShortcutId.CLOSE_TAB),
            act(ShortcutId.REOPEN_TAB),
            null,
        )
    }

    @Composable
    private fun DeckButton(
        action: MacroAction?,
        modifier: Modifier,
        onTap: () -> Unit,
        onLongPress: () -> Unit,
    ) {
        Box(
            modifier = modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .background(
                    if (action == null) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    RoundedCornerShape(18.dp),
                )
                .clickable(onClick = onTap)
                .combinedClickableLike(onLongPress)
                .padding(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.align(Alignment.Center)) {
                Text(
                    action?.label ?: "Empty",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (action == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (!action?.subtitle.isNullOrBlank()) {
                    Text(
                        action!!.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    /** Long-press support without androidx.compose.foundation.clickable duplication. */
    private fun Modifier.combinedClickableLike(onLongPress: () -> Unit): Modifier =
        this.then(
            Modifier.pointerInputCompat(onLongPress),
        )
}

@Composable
private fun SlotEditorDialog(
    existing: MacroAction?,
    onDismiss: () -> Unit,
    onSave: (MacroAction) -> Unit,
    onClear: () -> Unit,
) {
    val session = LocalSession.current
    val shortcuts = session.shortcuts()
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var kind by remember { mutableStateOf("shortcut") }
    var chosenShortcut by remember { mutableStateOf(ShortcutId.COPY) }
    var chosenMedia by remember { mutableStateOf(Keycodes.CONSUMER_PLAY_PAUSE) }
    var text by remember { mutableStateOf("") }
    var delayMs by remember { mutableStateOf("300") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New action" else "Edit action") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = kind == "shortcut", onClick = { kind = "shortcut" }, label = { Text("Shortcut") })
                    FilterChip(selected = kind == "media", onClick = { kind = "media" }, label = { Text("Media key") })
                    FilterChip(selected = kind == "text", onClick = { kind = "text" }, label = { Text("Text") })
                    FilterChip(selected = kind == "delay", onClick = { kind = "delay" }, label = { Text("Wait") })
                }
                when (kind) {
                    "shortcut" -> {
                        Text("Which shortcut?", style = MaterialTheme.typography.labelLarge)
                        Column {
                            ShortcutId.entries.forEach { id ->
                                Text(
                                    "${id.label}  (${shortcuts[id]?.describe()})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (chosenShortcut == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clickable { chosenShortcut = id }
                                        .padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                    "media" -> {
                        val mediaOptions = listOf(
                            "Play / Pause" to Keycodes.CONSUMER_PLAY_PAUSE,
                            "Next track" to Keycodes.CONSUMER_SCAN_NEXT,
                            "Previous track" to Keycodes.CONSUMER_SCAN_PREVIOUS,
                            "Mute" to Keycodes.CONSUMER_MUTE,
                            "Volume up" to Keycodes.CONSUMER_VOLUME_UP,
                            "Volume down" to Keycodes.CONSUMER_VOLUME_DOWN,
                        )
                        Column {
                            mediaOptions.forEach { (name, code) ->
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (chosenMedia == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clickable { chosenMedia = code }
                                        .padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                    "text" -> {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("Text to type") },
                            minLines = 2,
                        )
                    }
                    "delay" -> {
                        OutlinedTextField(
                            value = delayMs,
                            onValueChange = { delayMs = it.filter(Char::isDigit) },
                            label = { Text("Milliseconds") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val steps: List<MacroStep> = when (kind) {
                        "shortcut" -> {
                            val c = shortcuts[chosenShortcut] ?: return@TextButton
                            listOf(MacroStep.Chord(c.mods, c.usage))
                        }
                        "media" -> listOf(MacroStep.Media(chosenMedia))
                        "text" -> listOf(MacroStep.Text(text))
                        else -> listOf(MacroStep.Delay(delayMs.toLongOrNull() ?: 300))
                    }
                    val subtitle = when (kind) {
                        "shortcut" -> shortcuts[chosenShortcut]?.describe() ?: ""
                        "media" -> "Media key"
                        "text" -> "Types text"
                        else -> "Waits"
                    }
                    onSave(
                        MacroAction(
                            id = existing?.id ?: "a_${System.currentTimeMillis()}",
                            label = label.ifBlank { "Action" },
                            subtitle = subtitle,
                            steps = steps,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = onClear) { Text("Clear slot") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
