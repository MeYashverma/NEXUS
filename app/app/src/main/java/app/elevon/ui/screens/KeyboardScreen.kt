package app.elevon.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.data.HostLayout
import app.elevon.data.HapticsMode
import app.elevon.hid.HidConnectionState
import app.elevon.hid.Keycodes
import app.elevon.input.KeyDef
import app.elevon.input.KeyKind
import app.elevon.input.KeyLayouts
import app.elevon.input.KeyboardLayoutId
import app.elevon.ui.components.StateCard
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keyboard mode: Compact, Full, Gaming and Custom layouts over one HID
 * pairing. Now with full-screen landscape + rotate.
 *
 * Fullscreen: hides system bars, locks to landscape, keys fill entire screen,
 * floating back/exit buttons. Great for gaming and long typing.
 */
@Composable
fun KeyboardScreen(nav: NavHostController) {
    val session = LocalSession.current
    val connection by session.connection.collectAsState()
    val layoutPref by session.settings.keyboardLayout.value.collectAsState()
    val fnRow by session.settings.functionRow.value.collectAsState()
    val hostLayoutPref by session.settings.hostLayout.value.collectAsState()
    val haptics by session.settings.haptics.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val profile by session.activeProfile.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isSystemLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val hostLayout = remember(hostLayoutPref, profile) {
        runCatching { HostLayout.valueOf(hostLayoutPref) }.getOrDefault(HostLayout.US)
    }

    var capsLock by remember { mutableStateOf(false) }
    var sticky by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var isFullscreen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val connected = connection.connectionState == HidConnectionState.CONNECTED
    val useFullscreenLayout = isFullscreen || (isSystemLandscape && layoutPref == KeyboardLayoutId.FULL.name)

    // Fullscreen immersive handling
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

    fun haptic(long: Boolean = false) {
        when (haptics) {
            HapticsMode.OFF -> Unit
            HapticsMode.SUBTLE -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            HapticsMode.FULL ->
                hapticFeedback.performHapticFeedback(
                    if (long) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                )
        }
    }

    fun effectiveMods(): Int {
        var mods = 0
        keyModifierBits.forEach { (id, bit) ->
            if (sticky.containsKey(id)) mods = mods or bit
        }
        return mods
    }

    fun tapKey(key: KeyDef) {
        if (!connected) return
        when (key.kind) {
            KeyKind.MODIFIER -> {
                sticky = if (sticky.containsKey(key.id)) {
                    sticky - key.id
                } else {
                    sticky + (key.id to false)
                }
                haptic()
            }
            KeyKind.LOCK -> {
                session.capsLockToggle()
                capsLock = !capsLock
                haptic()
            }
            else -> {
                val mods = effectiveMods() and 0xFF
                session.hid.sendRawKeyboard(mods, listOf(key.usage))
                sticky = sticky.filterValues { it }
                haptic()
                scope.launch {
                    delay(12)
                    session.hid.sendRawKeyboard(0, emptyList())
                }
            }
        }
    }

    fun lockKey(key: KeyDef) {
        if (key.kind != KeyKind.MODIFIER) return
        sticky = if (sticky[key.id] == true) {
            sticky - key.id
        } else {
            sticky + (key.id to true)
        }
        haptic(long = true)
    }

    if (useFullscreenLayout) {
        // ---- FULLSCREEN LANDSCAPE KEYBOARD ----
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Floating top row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (isFullscreen) isFullscreen = false else nav.popBackStack() },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = hostLayout.label + " · " + layoutPref,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) {
                            Icon(
                                if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit fullscreen" else "Fullscreen"
                            )
                        }
                    }
                }

                // Keyboard fills remaining space
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentLayout = runCatching { KeyboardLayoutId.valueOf(layoutPref) }.getOrDefault(KeyboardLayoutId.COMPACT)
                    when (currentLayout) {
                        KeyboardLayoutId.COMPACT -> {
                            if (fnRow) FunctionStrip(::tapKey, capsLock)
                            KeyRowsFullscreen(KeyLayouts.compact, ::tapKey, ::lockKey, capsLock, sticky)
                        }
                        KeyboardLayoutId.FULL -> KeyRowsFullscreen(KeyLayouts.full, ::tapKey, ::lockKey, capsLock, sticky)
                        KeyboardLayoutId.GAMING -> {
                            if (fnRow) FunctionStrip(::tapKey, capsLock)
                            KeyRowsFullscreen(KeyLayouts.gaming, ::tapKey, ::lockKey, capsLock, sticky)
                        }
                        KeyboardLayoutId.CUSTOM -> CustomEditorFullscreen(::tapKey, ::lockKey, capsLock, sticky)
                    }
                }

                val active = sticky.keys + if (capsLock) listOf("caps") else emptyList()
                Text(
                    text = if (active.isEmpty()) "Tap modifier to stick, hold to lock · Fullscreen landscape" else "Held: " + active.joinToString(" · ") { it.replaceFirstChar(Char::uppercase) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        // ---- PORTRAIT DEFAULT ----
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Keyboard") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = hostLayout.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    IconButton(onClick = { isFullscreen = true }) {
                        Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen landscape")
                    }
                    IconButton(onClick = {
                        activity?.requestedOrientation = if (isSystemLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }) {
                        Icon(Icons.Outlined.ScreenRotation, contentDescription = "Rotate")
                    }
                },
            )

            if (!connected) {
                NotConnectedCard()
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KeyboardLayoutId.entries.forEach { layoutId ->
                        FilterChip(
                            selected = layoutPref == layoutId.name,
                            onClick = { session.settings.keyboardLayout.set(layoutId.name) },
                            label = { Text(layoutId.label) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Function row",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(checked = fnRow, onCheckedChange = { session.settings.functionRow.set(it) })
                }
                Spacer(Modifier.height(6.dp))

                when (runCatching { KeyboardLayoutId.valueOf(layoutPref) }.getOrDefault(KeyboardLayoutId.COMPACT)) {
                    KeyboardLayoutId.COMPACT -> {
                        if (fnRow) FunctionStrip(::tapKey, capsLock)
                        KeyRows(KeyLayouts.compact, ::tapKey, ::lockKey, capsLock, sticky)
                    }
                    KeyboardLayoutId.FULL -> KeyRows(KeyLayouts.full, ::tapKey, ::lockKey, capsLock, sticky)
                    KeyboardLayoutId.GAMING -> {
                        if (fnRow) FunctionStrip(::tapKey, capsLock)
                        KeyRows(KeyLayouts.gaming, ::tapKey, ::lockKey, capsLock, sticky)
                    }
                    KeyboardLayoutId.CUSTOM -> CustomEditor(::tapKey, ::lockKey, capsLock, sticky)
                }

                val active = sticky.keys + if (capsLock) listOf("caps") else emptyList()
                Text(
                    text = if (active.isEmpty()) {
                        "Tap a modifier to make it sticky, hold it to lock. Fullscreen button for landscape gaming."
                    } else {
                        "Held: " + active.joinToString(" · ") { it.replaceFirstChar(Char::uppercase) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun NotConnectedCard() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        StateCard(
            title = "No computer connected",
            body = "Keys are disabled until Elevon is connected. Connect from Home, then come straight back — your layout stays as you left it.",
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FunctionStrip(tap: (KeyDef) -> Unit, caps: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        KeyLayouts.functionRow.forEach { key ->
            KeyCapCell(
                key = key,
                pressed = false,
                locked = false,
                sticky = false,
                showShift = caps,
                onTap = tap,
                onLock = {},
                modifier = Modifier.weight(key.width),
            )
        }
    }
}

@Composable
private fun KeyRows(
    rows: List<List<KeyDef>>,
    tap: (KeyDef) -> Unit,
    lock: (KeyDef) -> Unit,
    caps: Boolean,
    sticky: Map<String, Boolean>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row.forEach { key ->
                    KeyCapCell(
                        key = key,
                        pressed = false,
                        locked = sticky[key.id] == true,
                        sticky = sticky.containsKey(key.id),
                        showShift = caps,
                        onTap = tap,
                        onLock = lock,
                        modifier = Modifier.weight(key.width.coerceAtLeast(0.1f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyRowsFullscreen(
    rows: List<List<KeyDef>>,
    tap: (KeyDef) -> Unit,
    lock: (KeyDef) -> Unit,
    caps: Boolean,
    sticky: Map<String, Boolean>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { key ->
                    KeyCapCell(
                        key = key,
                        pressed = false,
                        locked = sticky[key.id] == true,
                        sticky = sticky.containsKey(key.id),
                        showShift = caps,
                        onTap = tap,
                        onLock = lock,
                        modifier = Modifier.weight(key.width.coerceAtLeast(0.1f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyCapCell(
    key: KeyDef,
    pressed: Boolean,
    locked: Boolean,
    sticky: Boolean,
    showShift: Boolean,
    onTap: (KeyDef) -> Unit,
    onLock: (KeyDef) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val label = when {
        key.kind == KeyKind.CHAR && showShift -> key.label.uppercase()
        else -> key.label
    }
    val isModifier = key.kind == KeyKind.MODIFIER

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(key.id, isModifier) {
                coroutineScope {
                    while (true) {
                        val down = awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
                        down.consume()
                        isPressed = true
                        val holdStart = System.currentTimeMillis()

                        var released = false
                        val watcher = launch {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.all { !it.pressed }) {
                                        released = true
                                        break
                                    }
                                }
                            }
                        }

                        while (!released && System.currentTimeMillis() - holdStart < 420) delay(16)

                        if (released) {
                            isPressed = false
                            onTap(key)
                            watcher.cancel()
                            continue
                        }

                        if (isModifier) {
                            onLock(key)
                            watcher.join()
                        } else {
                            delay(400)
                            val start = System.currentTimeMillis()
                            while (!released && System.currentTimeMillis() - start < 6000) {
                                onTap(key)
                                delay(48)
                            }
                            watcher.cancel()
                        }
                        isPressed = false
                    }
                }
            }
            .border(
                1.dp,
                when {
                    locked -> MaterialTheme.colorScheme.primary
                    sticky -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    isPressed -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
                RoundedCornerShape(12.dp),
            )
            .background(
                when {
                    locked -> MaterialTheme.colorScheme.primaryContainer
                    isPressed -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (locked) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Composable
private fun CustomEditor(
    tap: (KeyDef) -> Unit,
    lock: (KeyDef) -> Unit,
    caps: Boolean,
    sticky: Map<String, Boolean>,
) {
    val session = LocalSession.current
    val saved by session.settings.customKeyboard.value.collectAsState()
    var editing by remember { mutableStateOf(CustomLayoutStore.decode(saved) == null) }
    var rows by remember(saved) {
        mutableStateOf(CustomLayoutStore.decode(saved) ?: CustomLayoutStore.default())
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (editing) {
            Text(
                "Tap a slot to change its key. Your layout is saved on this phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    row.forEachIndexed { colIndex, key ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                                .clickable {
                                    val next = CustomLayoutStore.cycleSlot(rows, rowIndex, colIndex)
                                    rows = next
                                    session.settings.customKeyboard.set(CustomLayoutStore.encode(next))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(key.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Done",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { editing = false }
                        .padding(8.dp),
                )
            }
        } else {
            KeyRows(rows, tap, lock, caps, sticky)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Edit layout",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { editing = true }
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun CustomEditorFullscreen(
    tap: (KeyDef) -> Unit,
    lock: (KeyDef) -> Unit,
    caps: Boolean,
    sticky: Map<String, Boolean>,
) {
    val session = LocalSession.current
    val saved by session.settings.customKeyboard.value.collectAsState()
    var rows by remember(saved) {
        mutableStateOf(CustomLayoutStore.decode(saved) ?: CustomLayoutStore.default())
    }
    KeyRowsFullscreen(rows, tap, lock, caps, sticky)
}

object CustomLayoutStore {

    val palette: List<KeyDef> = buildList {
        add(KeyDef.fn("esc", "Esc", Keycodes.KEY_ESC))
        add(KeyDef.fn("tab", "tab", Keycodes.KEY_TAB))
        add(KeyDef.fn("enter", "enter", Keycodes.KEY_ENTER))
        add(KeyDef.fn("bksp", "⌫", Keycodes.KEY_BACKSPACE))
        add(KeyDef.char("space", "space", Keycodes.KEY_SPACE))
        for (c in 'a'..'z') add(KeyDef.char(c.toString(), Keycodes.KEY_A + (c - 'a')))
        for (i in 1..9) add(KeyDef.char(i.toString(), Keycodes.KEY_1 + (i - 1)))
        add(KeyDef.char("0", "0", Keycodes.KEY_0))
        listOf('-', '=', '[', ']', '\\', ';', '\'', '`', ',', '.', '/').forEach { ch ->
            val usage = when (ch) {
                '-' -> Keycodes.KEY_MINUS
                '=' -> Keycodes.KEY_EQUAL
                '[' -> Keycodes.KEY_LEFTBRACKET
                ']' -> Keycodes.KEY_RIGHTBRACKET
                '\\' -> Keycodes.KEY_BACKSLASH
                ';' -> Keycodes.KEY_SEMICOLON
                '\'' -> Keycodes.KEY_APOSTROPHE
                '`' -> Keycodes.KEY_GRAVE
                ',' -> Keycodes.KEY_COMMA
                '.' -> Keycodes.KEY_DOT
                else -> Keycodes.KEY_SLASH
            }
            add(KeyDef.char(ch.toString(), usage))
        }
        for (i in 1..6) add(KeyDef.fn("f$i", "F$i", Keycodes.KEY_F1 + i - 1))
        add(KeyDef.fn("left", "←", Keycodes.KEY_LEFT))
        add(KeyDef.fn("right", "→", Keycodes.KEY_RIGHT))
        add(KeyDef.fn("up", "↑", Keycodes.KEY_UP))
        add(KeyDef.fn("down", "↓", Keycodes.KEY_DOWN))
    }

    fun default(): List<List<KeyDef>> = listOf(
        KeyLayouts.compact[0].take(9),
        KeyLayouts.compact[1].take(9),
        KeyLayouts.compact[2].take(9),
    )

    fun encode(rows: List<List<KeyDef>>): String =
        rows.joinToString("|") { row -> row.joinToString(",") { it.id } }

    fun decode(text: String): List<List<KeyDef>>? = runCatching {
        val byId = palette.associateBy { it.id }
        text.split("|").map { row ->
            row.split(",").map { id -> byId[id] ?: error("unknown key id $id") }
        }
    }.getOrNull()

    fun cycleSlot(rows: List<List<KeyDef>>, row: Int, col: Int): List<List<KeyDef>> {
        val current = rows.getOrNull(row)?.getOrNull(col)
        val index = palette.indexOfFirst { it.id == current?.id }
        val next = palette[(index + 1).mod(palette.size)]
        return rows.mapIndexed { r, rowDefs ->
            if (r == row) rowDefs.mapIndexed { c, k -> if (c == col) next else k } else rowDefs
        }
    }
}

private val keyModifierBits = listOf(
    "shift" to Keycodes.MOD_LSHIFT,
    "ctrl" to Keycodes.MOD_LCTRL,
    "alt" to Keycodes.MOD_LALT,
    "gui" to Keycodes.MOD_LGUI,
    "shift_r" to Keycodes.MOD_RSHIFT,
    "ctrl_r" to Keycodes.MOD_RCTRL,
)
