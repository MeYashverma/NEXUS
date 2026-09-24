package app.elevon.ui.screens

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.data.HapticsMode
import app.elevon.hid.MouseReport
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard
import kotlin.math.abs

/**
 * Touchpad mode. The whole screen is the surface; the minimal chrome floats.
 * Gestures: one finger move, two-finger scroll (vertical + horizontal),
 * tap-to-click, two-finger tap right-click, press-and-hold drag with drag
 * lock. Sensitivity, scroll speed, natural scrolling and tap-to-click are
 * user settings (research: scroll speed complaints were common).
 */
@Composable
fun TouchpadScreen(nav: NavHostController) {
    val session = LocalSession.current
    val connection by session.connection.collectAsState()
    val pointerSpeed by session.settings.pointerSpeed.value.collectAsState()
    val scrollSpeed by session.settings.scrollSpeed.value.collectAsState()
    val natural by session.settings.naturalScrolling.value.collectAsState()
    val tapToClick by session.settings.tapToClick.value.collectAsState()
    val dragLock by session.settings.dragLock.value.collectAsState()
    val haptics by session.settings.haptics.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    var showTuning by remember { mutableStateOf(false) }

    KeepScreenOnWhileVisible()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Touchpad") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                HonestyChip(Honesty.CORE, Modifier.padding(end = 4.dp))
                IconButton(onClick = { showTuning = !showTuning }) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Tune the touchpad")
                }
            },
        )

        if (connection.connectionState != app.elevon.hid.HidConnectionState.CONNECTED) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                StateCard(
                    title = "No computer connected",
                    body = "The touchpad is live only while connected. Connect from Home — this surface remembers your settings.",
                )
            }
        }

        if (showTuning) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text("Pointer speed", style = MaterialTheme.typography.labelLarge)
                Slider(value = pointerSpeed, onValueChange = { session.settings.pointerSpeed.set(it) }, valueRange = 0.4f..2.6f)
                Text("Scroll speed", style = MaterialTheme.typography.labelLarge)
                Slider(value = scrollSpeed, onValueChange = { session.settings.scrollSpeed.set(it) }, valueRange = 0.4f..3f)
                ToggleRow("Natural scrolling", "Scroll moves content, like a phone.", natural) {
                    session.settings.naturalScrolling.set(it)
                }
                ToggleRow("Tap to click", "A quick tap presses the left button.", tapToClick) {
                    session.settings.tapToClick.set(it)
                }
                ToggleRow("Drag lock", "After a tap-drag ends, the button stays down briefly so you can keep dragging.", dragLock) {
                    session.settings.dragLock.set(it)
                }
            }
        }

        // ---- the surface ----
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
                .pointerInput(pointerSpeed, scrollSpeed, natural, tapToClick, dragLock) {
                    val speed = pointerSpeed
                    val scroll = scrollSpeed
                    val naturalScroll = natural
                    val tapClick = tapToClick
                    val holdToDrag = dragLock
                    awaitEachGesture {
                        val first = awaitFirstDown(requireUnconsumed = false)
                        first.consume()
                        val idA = first.id
                        var lastA = first.position
                        var idB: PointerId? = null
                        var lastB: Offset? = null
                        var accMove = 0f
                        var accScroll = 0f
                        var twoFinger = false
                        var buttonHeld = false
                        val startTime = System.currentTimeMillis()

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) {
                                val duration = System.currentTimeMillis() - startTime
                                if (tapClick && !buttonHeld) {
                                    if (!twoFinger && accMove < 22f && duration < 230) {
                                        session.mouseButton(MouseReport.BUTTON_LEFT, true)
                                        session.mouseButton(MouseReport.BUTTON_LEFT, false)
                                        if (haptics != HapticsMode.OFF) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    } else if (twoFinger && accScroll < 22f && duration < 260) {
                                        session.mouseButton(MouseReport.BUTTON_RIGHT, true)
                                        session.mouseButton(MouseReport.BUTTON_RIGHT, false)
                                        if (haptics != HapticsMode.OFF) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                                if (buttonHeld) {
                                    session.mouseButton(MouseReport.BUTTON_LEFT, false)
                                }
                                break
                            }

                            // Second finger landing switches to scroll; lifting exits it.
                            if (!twoFinger && pressed.size >= 2) {
                                val second = pressed.firstOrNull { it.id != idA }
                                if (second != null) {
                                    idB = second.id
                                    lastB = second.position
                                    twoFinger = true
                                }
                            } else if (twoFinger && pressed.size < 2) {
                                twoFinger = false
                                idB = null
                                lastB = null
                            }

                            val changeA = event.changes.firstOrNull { it.id == idA }
                            val changeB = idB?.let { b -> event.changes.firstOrNull { it.id == b } }

                            val dA = if (changeA != null && changeA.pressed) changeA.position - lastA else Offset.Zero
                            val dB = if (changeB != null && changeB.pressed) {
                                val d = changeB.position - (lastB ?: changeB.position)
                                lastB = changeB.position
                                d
                            } else {
                                Offset.Zero
                            }
                            if (changeA != null && changeA.pressed) lastA = changeA.position

                            if (twoFinger) {
                                // Two-finger scroll: vertical = wheel, horizontal = AC Pan.
                                val avgY = (dA.y + dB.y) / 2f
                                val avgX = (dA.x + dB.x) / 2f
                                accScroll += kotlin.math.sqrt(avgY * avgY + avgX * avgX)
                                if (avgY != 0f) session.scroll(avgY / 10f, naturalScroll, scroll)
                                if (avgX != 0f) session.pan(avgX / 12f, scroll)
                                changeA?.consume()
                                changeB?.consume()
                            } else {
                                accMove += dA.getDistance()
                                session.mouseMove(dA.x, dA.y, speed)
                                // Press-and-hold with little movement becomes a drag.
                                if (!buttonHeld && holdToDrag && accMove < 14f &&
                                    System.currentTimeMillis() - startTime > 380
                                ) {
                                    session.mouseButton(MouseReport.BUTTON_LEFT, true)
                                    buttonHeld = true
                                    if (haptics != HapticsMode.OFF) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                                changeA?.consume()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (connection.connectionState == app.elevon.hid.HidConnectionState.CONNECTED) {
                Text(
                    "Move · tap to click · two fingers to scroll",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- buttons row ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MousePadButton("Left", Modifier.weight(1f)) { down ->
                session.mouseButton(MouseReport.BUTTON_LEFT, down)
            }
            MousePadButton("Middle", Modifier.weight(1f)) { down ->
                session.mouseButton(MouseReport.BUTTON_MIDDLE, down)
            }
            MousePadButton("Right", Modifier.weight(1f)) { down ->
                session.mouseButton(MouseReport.BUTTON_RIGHT, down)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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

/** Press-and-hold mouse button cell. */
@Composable
private fun MousePadButton(
    label: String,
    modifier: Modifier = Modifier,
    onHold: (Boolean) -> Unit,
) {
    var held by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(56.dp)
            .border(
                1.dp,
                if (held) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp),
            )
            .background(
                if (held) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(16.dp),
            )
            .pointerInput(label) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    held = true
                    onHold(true)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.all { !it.pressed }) break
                    }
                    held = false
                    onHold(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Keeps the screen on while a control mode is open (setting-controlled). */
@Composable
fun KeepScreenOnWhileVisible() {
    val session = LocalSession.current
    val keep by session.settings.keepScreenOn.collectAsState()
    val context = LocalContext.current
    DisposableEffect(keep) {
        val window = (context as? android.app.Activity)?.window
        if (keep) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
