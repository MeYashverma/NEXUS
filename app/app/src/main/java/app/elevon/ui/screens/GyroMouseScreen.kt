package app.elevon.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.elevon.data.GyroMode
import app.elevon.hid.HidConnectionState
import app.elevon.hid.MouseReport
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard
import kotlin.math.abs

/**
 * Gyro Mouse — Labs experimental: air mouse via gyroscope.
 * Hold phone like a remote, tilt to move cursor, tap to click.
 * Fullscreen landscape support for couch use.
 */
@Composable
fun GyroMouseScreen(nav: NavHostController) {
    val session = LocalSession.current
    val connection by session.connection.collectAsState()
    val gyroSens by session.settings.gyroSensitivity.value.collectAsState()
    val gyroModePref by session.settings.gyroMode.value.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isSystemLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isFullscreen by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableStateOf(gyroSens) }
    val haptic = LocalHapticFeedback.current
    val connected = connection.connectionState == HidConnectionState.CONNECTED
    val useFullscreen = isFullscreen || isSystemLandscape

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

    // Gyro sensor handling
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val gyroSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
    var lastUpdate by remember { mutableStateOf(0L) }

    DisposableEffect(isActive, sensitivity, connected) {
        if (!isActive || !connected) {
            return@DisposableEffect onDispose {}
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()
                if (now - lastUpdate < 16) return // ~60Hz
                lastUpdate = now
                val x = event.values[0] // pitch
                val y = event.values[1] // roll
                val z = event.values[2] // yaw
                // Use yaw and pitch for mouse movement, invert as needed
                val dx = (-y * sensitivity * 40f).toInt().coerceIn(-30, 30)
                val dy = (x * sensitivity * 40f).toInt().coerceIn(-30, 30)
                if (abs(dx) > 1 || abs(dy) > 1) {
                    session.mouseMove(dx.toFloat(), dy.toFloat(), 1f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        gyroSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    @Composable
    fun ControlsRow() {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(88.dp)
                    .border(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                    .pointerInput(isActive) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isActive = !isActive
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            while (true) {
                                val ev = awaitPointerEvent()
                                if (ev.changes.all { !it.pressed }) break
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Sensors, contentDescription = null, tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (isActive) "Gyro ON" else "Tap to enable", style = MaterialTheme.typography.labelLarge)
                    Text(if (isActive) "Tilt to move" else "Air mouse", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(88.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false).consume()
                            session.mouseButton(MouseReport.BUTTON_LEFT, true)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            while (true) {
                                val ev = awaitPointerEvent()
                                if (ev.changes.all { !it.pressed }) break
                            }
                            session.mouseButton(MouseReport.BUTTON_LEFT, false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Hold to click", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (useFullscreen) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (isFullscreen) isFullscreen = false else nav.popBackStack() },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HonestyChip(Honesty.EXPERIMENTAL, Modifier)
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) { Icon(if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen, contentDescription = "Fullscreen") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (!connected) {
                    StateCard(title = "No computer connected", body = "Connect from Home to use gyro mouse. Experimental Labs feature — tilt phone to move cursor.")
                    Spacer(Modifier.height(12.dp))
                }
                Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isActive) "Gyro active — tilt phone" else "Tap Enable to start gyro", style = MaterialTheme.typography.titleMedium)
                        Text("Sensitivity: ${"%.1f".format(sensitivity)}x — adjust below", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                ControlsRow()
                Spacer(Modifier.height(8.dp))
                Text("Sensitivity", style = MaterialTheme.typography.labelLarge)
                Slider(value = sensitivity, onValueChange = { sensitivity = it; session.settings.gyroSensitivity.set(it) }, valueRange = 0.2f..3f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(56.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)).pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            session.mouseButton(MouseReport.BUTTON_LEFT, true)
                            session.mouseButton(MouseReport.BUTTON_LEFT, false)
                            while (true) { val ev = awaitPointerEvent(); if (ev.changes.all { !it.pressed }) break }
                        }
                    }, contentAlignment = Alignment.Center) { Text("Left click") }
                    Box(Modifier.weight(1f).height(56.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)).pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            session.mouseButton(MouseReport.BUTTON_RIGHT, true)
                            session.mouseButton(MouseReport.BUTTON_RIGHT, false)
                            while (true) { val ev = awaitPointerEvent(); if (ev.changes.all { !it.pressed }) break }
                        }
                    }, contentAlignment = Alignment.Center) { Text("Right click") }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Gyro Mouse") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") } },
                actions = {
                    HonestyChip(Honesty.EXPERIMENTAL, Modifier.padding(end = 4.dp))
                    IconButton(onClick = { isFullscreen = true }) { Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen") }
                    IconButton(onClick = { activity?.requestedOrientation = if (isSystemLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }) {
                        Icon(Icons.Outlined.ScreenRotation, contentDescription = "Rotate")
                    }
                }
            )
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                if (!connected) {
                    StateCard(title = "No computer connected", body = "Connect from Home to use gyro mouse. Experimental — uses gyroscope to move cursor like an air remote.")
                    Spacer(Modifier.height(12.dp))
                }
                Box(Modifier.fillMaxWidth().height(180.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isActive) "Gyro active" else "Gyro idle", style = MaterialTheme.typography.titleMedium)
                        Text(if (gyroSensor == null) "No gyroscope found" else "Tilt phone to move cursor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                ControlsRow()
                Spacer(Modifier.height(16.dp))
                Text("Sensitivity", style = MaterialTheme.typography.labelLarge)
                Slider(value = sensitivity, onValueChange = { sensitivity = it; session.settings.gyroSensitivity.set(it) }, valueRange = 0.2f..3f)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { session.mouseButton(MouseReport.BUTTON_LEFT, true); session.mouseButton(MouseReport.BUTTON_LEFT, false) }, modifier = Modifier.weight(1f)) { Text("Left") }
                    Button(onClick = { session.mouseButton(MouseReport.BUTTON_RIGHT, true); session.mouseButton(MouseReport.BUTTON_RIGHT, false) }, modifier = Modifier.weight(1f)) { Text("Right") }
                    Button(onClick = { session.scroll(10f, false, 1f) }, modifier = Modifier.weight(1f)) { Text("Scroll") }
                }
                Spacer(Modifier.height(16.dp))
                Text("How it works: gyroscope reports rotation rate. Elevon converts yaw/pitch to mouse deltas. Keep elbow steady, rotate wrist. Experimental — accuracy varies by phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
