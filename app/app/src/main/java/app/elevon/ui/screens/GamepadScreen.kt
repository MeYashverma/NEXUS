package app.elevon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.data.HapticsMode
import app.elevon.hid.HidConnectionState
import app.elevon.input.GamepadProfile
import app.elevon.input.GamepadProfiles
import app.elevon.input.PadElement
import app.elevon.input.PadKind
import app.elevon.input.PadOutput
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Gamepad mode. Default layout mirrors a standard controller; profiles adapt
 * it per game. Output modes are honest: GAMEPAD sends real HID gamepad
 * reports, KEYBOARD_MOUSE sends keys/mouse so games without controller
 * support still work (docs/compatibility.md).
 */
@Composable
fun GamepadScreen(nav: NavHostController) {
    val session = LocalSession.current
    val connection by session.connection.collectAsState()
    val haptics by session.settings.haptics.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val customPads by session.repos.pads.collectAsState()

    val allPads = remember(customPads) { GamepadProfiles.defaults() + customPads }
    var selectedPadId by remember { mutableStateOf(allPads.firstOrNull()?.id ?: "std") }
    var pad by remember(selectedPadId, customPads) {
        mutableStateOf(allPads.firstOrNull { it.id == selectedPadId } ?: GamepadProfiles.standard())
    }
    var editMode by remember { mutableStateOf(false) }
    var selectedElement by remember { mutableStateOf<String?>(null) }

    val connected = connection.connectionState == HidConnectionState.CONNECTED

    androidx.compose.runtime.LaunchedEffect(pad) { session.gamepad.setProfile(pad) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        session.gamepad.start()
        onDispose { session.gamepad.stop() }
    }

    fun haptic() {
        if (haptics == HapticsMode.FULL) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Gamepad") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                HonestyChip(Honesty.CORE, Modifier.padding(end = 4.dp))
                IconButton(onClick = { editMode = !editMode; selectedElement = null }) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = if (editMode) "Finish editing layout" else "Edit layout",
                        tint = if (editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        if (!connected) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                StateCard(
                    title = "No computer connected",
                    body = "Connect from Home to send inputs. You can still edit layouts now.",
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            // Profile chips
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                allPads.forEach { p ->
                    FilterChip(
                        selected = p.id == pad.id,
                        onClick = {
                            selectedPadId = p.id
                            pad = p
                        },
                        label = { Text(p.name) },
                    )
                }
            }

            // Output mode + tuning
            val mode = pad.outputMode
            Text(
                when (mode) {
                    GamepadProfile.OutputMode.GAMEPAD ->
                        "Output: real gamepad. Steam and controller-aware games work best; XInput-only games need Steam Input."
                    GamepadProfile.OutputMode.KEYBOARD_MOUSE ->
                        "Output: keyboard & mouse. Works with every game, including ones with no controller support."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Text("Dead zone", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = pad.deadZone,
                onValueChange = { pad = pad.copy(deadZone = it) },
                valueRange = 0f..0.4f,
                onValueChangeFinished = { session.repos.upsertPad(pad); selectedPadId = pad.id },
            )
            Text("Sensitivity", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = pad.sensitivity,
                onValueChange = { pad = pad.copy(sensitivity = it) },
                valueRange = 0.4f..2.5f,
                onValueChangeFinished = { session.repos.upsertPad(pad); selectedPadId = pad.id },
            )

            if (editMode) {
                Text(
                    "Drag controls to move them. Tap one to resize with the slider below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                selectedElement?.let { id ->
                    val el = pad.element(id)
                    if (el != null) {
                        Text("Size · ${el.label}", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = el.size,
                            onValueChange = { v ->
                                pad = pad.withElement(el.copy(size = v))
                            },
                            valueRange = 0.6f..1.8f,
                            onValueChangeFinished = {
                                session.repos.upsertPad(pad)
                                selectedPadId = pad.id
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---- the pad surface ----
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                val w = maxWidth
                val h = maxHeight
                pad.elements.forEach { element ->
                    val selected = selectedElement == element.id
                    val modifier = Modifier.offset {
                        IntOffset(
                            (element.x * w.toPx() - 34.dp.toPx()).roundToInt(),
                            (element.y * h.toPx() - 34.dp.toPx()).roundToInt(),
                        )
                    }
                    when (element.kind) {
                        PadKind.STICK -> StickControl(
                            element = element,
                            editMode = editMode,
                            selected = selected,
                            modifier = modifier,
                            onMove = { x, y -> session.gamepad.setStick(stickIndex(element), x, y) },
                            onKeyboardDirs = { dirs ->
                                val chords = (element.output as? PadOutput.Chords)?.chords
                                if (chords != null) session.gamepad.keyboardStickDirs(stickIndex(element), dirs, chords)
                            },
                            onStickClick = { down ->
                                session.gamepad.setButton(if (stickIndex(element) == 0) 6 else 7, down)
                            },
                            onClickEdit = { selectedElement = element.id },
                            onDrag = { dx, dy ->
                                pad = pad.withElement(
                                    element.copy(
                                        x = (element.x + dx / w.value).coerceIn(0.05f, 0.95f),
                                        y = (element.y + dy / h.value).coerceIn(0.05f, 0.95f),
                                    ),
                                )
                            },
                            onCommitEdit = {
                                session.repos.upsertPad(pad)
                                selectedPadId = pad.id
                            },
                        )
                        PadKind.DPAD -> DpadControl(
                            element = element,
                            editMode = editMode,
                            selected = selected,
                            modifier = modifier,
                            onDirs = { dirs ->
                                session.gamepad.setDpad(dirs)
                                val chords = (element.output as? PadOutput.Chords)?.chords
                                if (chords != null) session.gamepad.keyboardStickDirs(-1, dirs, chords)
                            },
                            onClickEdit = { selectedElement = element.id },
                            onDrag = { dx, dy ->
                                pad = pad.withElement(
                                    element.copy(
                                        x = (element.x + dx / w.value).coerceIn(0.05f, 0.95f),
                                        y = (element.y + dy / h.value).coerceIn(0.05f, 0.95f),
                                    ),
                                )
                            },
                            onCommitEdit = {
                                session.repos.upsertPad(pad)
                                selectedPadId = pad.id
                            },
                        )
                        PadKind.TRIGGER -> TriggerControl(
                            element = element,
                            editMode = editMode,
                            selected = selected,
                            modifier = modifier,
                            onValue = { v -> session.gamepad.setTrigger(triggerIndex(element), v) },
                            onClickEdit = { selectedElement = element.id },
                            onDrag = { dx, dy ->
                                pad = pad.withElement(
                                    element.copy(
                                        x = (element.x + dx / w.value).coerceIn(0.05f, 0.95f),
                                        y = (element.y + dy / h.value).coerceIn(0.03f, 0.4f),
                                    ),
                                )
                            },
                            onCommitEdit = {
                                session.repos.upsertPad(pad)
                                selectedPadId = pad.id
                            },
                        )
                        PadKind.BUTTON -> ButtonControl(
                            element = element,
                            editMode = editMode,
                            selected = selected,
                            modifier = modifier,
                            onPress = { down ->
                                session.gamepad.pressElement(element.id, down)
                                haptic()
                            },
                            onClickEdit = { selectedElement = element.id },
                            onDrag = { dx, dy ->
                                pad = pad.withElement(
                                    element.copy(
                                        x = (element.x + dx / w.value).coerceIn(0.03f, 0.97f),
                                        y = (element.y + dy / h.value).coerceIn(0.03f, 0.97f),
                                    ),
                                )
                            },
                            onCommitEdit = {
                                session.repos.upsertPad(pad)
                                selectedPadId = pad.id
                            },
                        )
                    }
                }
            }

            Text(
                "L3 / R3: double-tap a stick. Trigger rows: drag to pull.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun stickIndex(element: PadElement): Int =
    (element.output as? PadOutput.PadStick)?.stick ?: 0

private fun triggerIndex(element: PadElement): Int =
    (element.output as? PadOutput.PadTrigger)?.index ?: 0

/** Analogue stick: drag the knob; double-tap = stick click. */
@Composable
private fun StickControl(
    element: PadElement,
    editMode: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onMove: (Float, Float) -> Unit,
    onKeyboardDirs: (Set<Int>) -> Unit,
    onStickClick: (Boolean) -> Unit,
    onClickEdit: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onCommitEdit: () -> Unit,
) {
    val base = 68.dp * element.size
    var knob by remember { mutableStateOf(Offset.Zero) }
    var lastTapAt by remember { mutableStateOf(0L) }
    val radius = with(androidx.compose.ui.platform.LocalDensity.current) { base.toPx() / 2f }

    Box(
        modifier = modifier
            .size(base * 1.7f)
            .offset { IntOffset(-(base * 0.35f).toPx().roundToInt(), -(base * 0.35f).toPx().roundToInt()) }
            .then(
                if (editMode) {
                    Modifier.pointerInput(element.id) {
                        detectDragGestures(
                            onDragStart = { onClickEdit() },
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(amount.x, amount.y)
                            },
                            onDragEnd = { onCommitEdit() },
                        )
                    }
                } else {
                    Modifier.pointerInput(element.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val now = System.currentTimeMillis()
                            val isDoubleTap = now - lastTapAt < 300
                            lastTapAt = now
                            if (isDoubleTap) {
                                // Stick click: momentary L3/R3 (buttons 6/7).
                                val clickBit = if (stickIndex(element) == 0) 6 else 7
                                onStickClick(true)
                            }
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    knob = Offset.Zero
                                    onMove(0f, 0f)
                                    onKeyboardDirs(emptySet())
                                    if (isDoubleTap) {
                                        onStickClick(false)
                                    }
                                    break
                                }
                                val delta = change.position - change.previousPosition
                                if (editMode) {
                                    onDrag(delta.x, delta.y)
                                } else {
                                    val next = knob + delta
                                    val dist = sqrt(next.x * next.x + next.y * next.y)
                                    val clamped = if (dist > radius) next * (radius / dist) else next
                                    knob = clamped
                                    val nx = clamped.x / radius
                                    val ny = clamped.y / radius
                                    onMove(nx, ny)
                                    // Digital directions for keyboard mode.
                                    val dirs = buildSet {
                                        if (ny < -0.45f) add(0)
                                        if (nx > 0.45f) add(1)
                                        if (ny > 0.45f) add(2)
                                        if (nx < -0.45f) add(3)
                                    }
                                    onKeyboardDirs(dirs)
                                }
                                change.consume()
                            }
                        }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(base * 1.7f)
                .border(
                    2.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape,
                )
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(base)
                    .offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape,
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

private fun clickBit2(element: PadElement): Int = if (stickIndex(element) == 0) 6 else 7

/** 8-direction D-pad: zones by angle. */
@Composable
private fun DpadControl(
    element: PadElement,
    editMode: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onDirs: (Set<Int>) -> Unit,
    onClickEdit: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onCommitEdit: () -> Unit,
) {
    val size = 84.dp * element.size
    Box(
        modifier = modifier
            .size(size)
            .offset { IntOffset(-(size / 2).toPx().roundToInt(), -(size / 2).toPx().roundToInt()) }
            .then(
                if (editMode) {
                    Modifier.pointerInput(element.id) {
                        detectDragGestures(
                            onDragStart = { onClickEdit() },
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(amount.x, amount.y)
                            },
                            onDragEnd = { onCommitEdit() },
                        )
                    }
                } else {
                    Modifier.pointerInput(element.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var dirs: Set<Int> = emptySet()
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) {
                                    onDirs(emptySet())
                                    break
                                }
                                val p = pressed.first()
                                val center = event.changes.first().position
                                val dx = p.position.x - center.x
                                val dy = p.position.y - center.y
                                val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))
                                dirs = buildSet {
                                    if (angle in -157.5..-22.5) add(3) // left? atan2 y down...
                                    if (angle in 22.5..157.5) add(2) // down
                                    if (angle < -157.5 || angle > 157.5) add(3)
                                    if (angle in -22.5..22.5) add(1)
                                    if (angle in -112.5..-67.5) add(0) // up
                                }
                                onDirs(dirs)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Simple visual cross.
        Box(
            Modifier
                .size(size)
                .border(
                    2.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(element.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Vertical trigger slider (pull down). */
@Composable
private fun TriggerControl(
    element: PadElement,
    editMode: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onValue: (Float) -> Unit,
    onClickEdit: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onCommitEdit: () -> Unit,
) {
    var value by remember { mutableStateOf(0f) }
    val height = 64.dp * element.size
    Box(
        modifier = modifier
            .size(width = 34.dp * element.size, height = height)
            .offset { IntOffset(-(17.dp * element.size).toPx().roundToInt(), -(height / 2).toPx().roundToInt()) }
            .then(
                if (editMode) {
                    Modifier.pointerInput(element.id) {
                        detectDragGestures(
                            onDragStart = { onClickEdit() },
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(amount.x, amount.y)
                            },
                            onDragEnd = { onCommitEdit() },
                        )
                    }
                } else {
                    Modifier.pointerInput(element.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) {
                                    value = 0f
                                    onValue(0f)
                                    break
                                }
                                val p = pressed.first().position
                                value = 1f
                                onValue(1f)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .border(
                    2.dp,
                    if (selected || value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(12.dp),
                )
                .background(
                    if (value > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(element.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Face/shoulder/system button. */
@Composable
private fun ButtonControl(
    element: PadElement,
    editMode: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onPress: (Boolean) -> Unit,
    onClickEdit: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onCommitEdit: () -> Unit,
) {
    var held by remember { mutableStateOf(false) }
    val side = (44.dp * element.size).coerceAtLeast(30.dp)
    Box(
        modifier = modifier
            .size(side)
            .offset { IntOffset(-(side / 2).toPx().roundToInt(), -(side / 2).toPx().roundToInt()) }
            .then(
                if (editMode) {
                    Modifier.pointerInput(element.id) {
                        detectDragGestures(
                            onDragStart = { onClickEdit() },
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(amount.x, amount.y)
                            },
                            onDragEnd = { onCommitEdit() },
                        )
                    }
                } else {
                    Modifier.pointerInput(element.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            held = true
                            onPress(true)
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { !it.pressed }) break
                                event.changes.forEach { it.consume() }
                            }
                            held = false
                            onPress(false)
                        }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .border(
                    2.dp,
                    when {
                        held || selected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    CircleShape,
                )
                .background(
                    if (held) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(element.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
