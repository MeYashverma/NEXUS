package app.elevon.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
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
import app.elevon.data.AccelerationCurve
import app.elevon.data.HapticsMode
import app.elevon.hid.MouseReport
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Touchpad mode — now with acceleration curves and fullscreen landscape.
 * The whole screen is the surface; chrome floats. New: pointer curves (linear, ease-out, precise, gaming)
 * and fullscreen landscape for couch use.
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
    val pointerCurvePref by session.settings.pointerCurve.value.collectAsState()
    val haptics by session.settings.haptics.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    var showTuning by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isSystemLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val curve = remember(pointerCurvePref) {
        runCatching { AccelerationCurve.valueOf(pointerCurvePref) }.getOrDefault(AccelerationCurve.LINEAR)
    }

    KeepScreenOnWhileVisible()

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

    fun applyCurve(dx: Float, dy: Float, speed: Float, curve: AccelerationCurve): Pair<Float, Float> {
        val dist = sqrt(dx * dx + dy * dy)
        val factor = when (curve) {
            AccelerationCurve.LINEAR -> speed
            AccelerationCurve.EASE_OUT -> speed * (1f + sqrt(dist) * 0.08f)
            AccelerationCurve.EASE_IN_OUT -> {
                val t = (dist / 40f).coerceIn(0f, 1f)
                val eased = t * t * (3f - 2f * t) // smoothstep
                speed * (0.6f + eased * 0.9f)
            }
            AccelerationCurve.PRECISE -> {
                if (dist < 8f) speed * 0.5f else speed * (1f + (dist - 8f) * 0.04f)
            }
            AccelerationCurve.GAMING -> {
                if (dist > 12f) speed * 1.35f else speed
            }
        }
        return (dx * factor) to (dy * factor)
    }

    if (isFullscreen || isSystemLandscape) {
        // Fullscreen landscape touchpad
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            // Surface
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
                    .pointerInput(pointerSpeed, scrollSpeed, natural, tapToClick, dragLock, curve) {
                        val speed = pointerSpeed
                        val scroll = scrollSpeed
                        val naturalScroll = natural
                        val tapClick = tapToClick
                        val holdToDrag = dragLock
                        val currentCurve = curve
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
                                            if (haptics != HapticsMode.OFF) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (twoFinger && accScroll < 22f && duration < 260) {
                                            session.mouseButton(MouseReport.BUTTON_RIGHT, true)
                                            session.mouseButton(MouseReport.BUTTON_RIGHT, false)
                                            if (haptics != HapticsMode.OFF) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    if (buttonHeld) session.mouseButton(MouseReport.BUTTON_LEFT, false)
                                    break
                                }
                                if (!twoFinger && pressed.size >= 2) {
                                    val second = pressed.firstOrNull { it.id != idA }
                                    if (second != null) { idB = second.id; lastB = second.position; twoFinger = true }
                                } else if (twoFinger && pressed.size < 2) {
                                    twoFinger = false; idB = null; lastB = null
                                }
                                val changeA = event.changes.firstOrNull { it.id == idA }
                                val changeB = idB?.let { b -> event.changes.firstOrNull { it.id == b } }
                                val dA = if (changeA != null && changeA.pressed) changeA.position - lastA else Offset.Zero
                                val dB = if (changeB != null && changeB.pressed) {
                                    val d = changeB.position - (lastB ?: changeB.position); lastB = changeB.position; d
                                } else Offset.Zero
                                if (changeA != null && changeA.pressed) lastA = changeA.position
                                if (twoFinger) {
                                    val avgY = (dA.y + dB.y) / 2f
                                    val avgX = (dA.x + dB.x) / 2f
                                    accScroll += sqrt(avgY * avgY + avgX * avgX)
                                    if (avgY != 0f) session.scroll(avgY / 10f, naturalScroll, scroll)
                                    if (avgX != 0f) session.pan(avgX / 12f, scroll)
                                    changeA?.consume(); changeB?.consume()
                                } else {
                                    accMove += dA.getDistance()
                                    val (cx, cy) = applyCurve(dA.x, dA.y, speed, currentCurve)
                                    session.mouseMove(cx, cy, 1f)
                                    if (!buttonHeld && holdToDrag && accMove < 14f && System.currentTimeMillis() - startTime > 380) {
                                        session.mouseButton(MouseReport.BUTTON_LEFT, true); buttonHeld = true
                                        if (haptics != HapticsMode.OFF) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    changeA?.consume()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Fullscreen touchpad · ${curve.label} · move, tap, two-finger scroll", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Floating controls
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (isFullscreen) isFullscreen = false else nav.popBackStack() }, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showTuning = !showTuning }, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Tune")
                    }
                    IconButton(onClick = { isFullscreen = !isFullscreen }, modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) {
                        Icon(if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen, contentDescription = "Fullscreen")
                    }
                }
            }
        }
    } else {
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
                    IconButton(onClick = { isFullscreen = true }) { Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen landscape") }
                    IconButton(onClick = { activity?.requestedOrientation = if (isSystemLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }) {
                        Icon(Icons.Outlined.ScreenRotation, contentDescription = "Rotate")
                    }
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
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text("Pointer speed", style = MaterialTheme.typography.labelLarge)
                    Slider(value = pointerSpeed, onValueChange = { session.settings.pointerSpeed.set(it) }, valueRange = 0.4f..2.6f)
                    Text("Scroll speed", style = MaterialTheme.typography.labelLarge)
                    Slider(value = scrollSpeed, onValueChange = { session.settings.scrollSpeed.set(it) }, valueRange = 0.4f..3f)
                    Text("Acceleration curve — new Labs feature", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccelerationCurve.entries.forEach { c ->
                            FilterChip(selected = curve == c, onClick = { session.settings.pointerCurve.set(c.name) }, label = { Text(c.label) })
                        }
                    }
                    Text(curve.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
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

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp))
                    .pointerInput(pointerSpeed, scrollSpeed, natural, tapToClick, dragLock, curve) {
                        val speed = pointerSpeed
                        val scroll = scrollSpeed
                        val naturalScroll = natural
                        val tapClick = tapToClick
                        val holdToDrag = dragLock
                        val currentCurve = curve
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
                                            if (haptics != HapticsMode.OFF) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (twoFinger && accScroll < 22f && duration < 260) {
                                            session.mouseButton(MouseReport.BUTTON_RIGHT, true)
                                            session.mouseButton(MouseReport.BUTTON_RIGHT, false)
                                            if (haptics != HapticsMode.OFF) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    if (buttonHeld) session.mouseButton(MouseReport.BUTTON_LEFT, false)
                                    break
                                }
                                if (!twoFinger && pressed.size >= 2) {
                                    val second = pressed.firstOrNull { it.id != idA }
                                    if (second != null) { idB = second.id; lastB = second.position; twoFinger = true }
                                } else if (twoFinger && pressed.size < 2) {
                                    twoFinger = false; idB = null; lastB = null
                                }
                                val changeA = event.changes.firstOrNull { it.id == idA }
                                val changeB = idB?.let { b -> event.changes.firstOrNull { it.id == b } }
                                val dA = if (changeA != null && changeA.pressed) changeA.position - lastA else Offset.Zero
                                val dB = if (changeB != null && changeB.pressed) {
                                    val d = changeB.position - (lastB ?: changeB.position); lastB = changeB.position; d
                                } else Offset.Zero
                                if (changeA != null && changeA.pressed) lastA = changeA.position
                                if (twoFinger) {
                                    val avgY = (dA.y + dB.y) / 2f
                                    val avgX = (dA.x + dB.x) / 2f
                                    accScroll += kotlin.math.sqrt(avgY * avgY + avgX * avgX)
                                    if (avgY != 0f) session.scroll(avgY / 10f, naturalScroll, scroll)
                                    if (avgX != 0f) session.pan(avgX / 12f, scroll)
                                    changeA?.consume(); changeB?.consume()
                                } else {
                                    accMove += dA.getDistance()
                                    val (cx, cy) = applyCurve(dA.x, dA.y, speed, currentCurve)
                                    session.mouseMove(cx, cy, 1f)
                                    if (!buttonHeld && holdToDrag && accMove < 14f && System.currentTimeMillis() - startTime > 380) {
                                        session.mouseButton(MouseReport.BUTTON_LEFT, true); buttonHeld = true
                                        if (haptics != HapticsMode.OFF) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        "Move · tap to click · two fingers to scroll · ${curve.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MousePadButton("Left", Modifier.weight(1f)) { down -> session.mouseButton(MouseReport.BUTTON_LEFT, down) }
                MousePadButton("Middle", Modifier.weight(1f)) { down -> session.mouseButton(MouseReport.BUTTON_MIDDLE, down) }
                MousePadButton("Right", Modifier.weight(1f)) { down -> session.mouseButton(MouseReport.BUTTON_RIGHT, down) }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun MousePadButton(label: String, modifier: Modifier = Modifier, onHold: (Boolean) -> Unit) {
    var held by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    Box(
        modifier = modifier.height(56.dp).border(1.dp, if (held) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).background(if (held) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)).pointerInput(label) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false).consume()
                held = true; onHold(true); hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                while (true) { val event = awaitPointerEvent(PointerEventPass.Main); if (event.changes.all { !it.pressed }) break }
                held = false; onHold(false)
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun KeepScreenOnWhileVisible() {
    val session = LocalSession.current
    val keep by session.settings.keepScreenOn.value.collectAsState()
    val context = LocalContext.current
    DisposableEffect(keep) {
        val window = (context as? android.app.Activity)?.window
        if (keep) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}
