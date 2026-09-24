package app.elevon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.LocalSession
import app.elevon.data.HapticsMode
import app.elevon.hid.HidConnectionState
import app.elevon.hid.Keycodes
import app.elevon.ui.components.Honesty
import app.elevon.ui.components.HonestyChip
import app.elevon.ui.components.StateCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Media and presentation remotes: big targets, built for away-from-desk use. */
object RemoteScreens {

    // ---- Media -------------------------------------------------------------

    @Composable
    fun mediaScreen(nav: NavHostController) {
        val session = LocalSession.current
        val connection by session.connection.collectAsState()
        KeepScreenOnWhileVisible()

        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Media") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { HonestyChip(Honesty.CORE, Modifier.padding(end = 12.dp)) },
            )
            if (connection.connectionState != HidConnectionState.CONNECTED) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    StateCard(
                        title = "No computer connected",
                        body = "Media keys reach the computer only while connected.",
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MediaButton(Icons.Outlined.SkipPrevious, "Previous", Modifier.weight(1f)) {
                        session.sendConsumer(Keycodes.CONSUMER_SCAN_PREVIOUS)
                    }
                    MediaButton(Icons.Outlined.PlayArrow, "Play / Pause", Modifier.weight(1.4f), primary = true) {
                        session.sendConsumer(Keycodes.CONSUMER_PLAY_PAUSE)
                    }
                    MediaButton(Icons.Outlined.SkipNext, "Next", Modifier.weight(1f)) {
                        session.sendConsumer(Keycodes.CONSUMER_SCAN_NEXT)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HoldButton("Volume −", Modifier.weight(1f)) {
                        session.sendConsumer(Keycodes.CONSUMER_VOLUME_DOWN)
                    }
                    HoldButton("Mute", Modifier.weight(1f)) {
                        session.sendConsumer(Keycodes.CONSUMER_MUTE)
                    }
                    HoldButton("Volume +", Modifier.weight(1f)) {
                        session.sendConsumer(Keycodes.CONSUMER_VOLUME_UP)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HoldButton("◀◀ Rewind", Modifier.weight(1f), icon = Icons.Outlined.FastRewind) {
                        session.sendConsumer(Keycodes.CONSUMER_REWIND)
                    }
                    HoldButton("Forward ▶▶", Modifier.weight(1f), icon = Icons.Outlined.FastForward) {
                        session.sendConsumer(Keycodes.CONSUMER_FAST_FORWARD)
                    }
                }
                Text(
                    "Works with any app that responds to standard media keys — players, browsers, streaming sites.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // ---- Presentation --------------------------------------------------------

    private enum class AdvanceMode(val label: String, val note: String) {
        ARROWS("Arrow keys", "Works in PowerPoint, Keynote, Impress, Google Slides and most viewers."),
        PAGE("Page keys", "Page Down / Page Up — handy for PDF readers."),
        NP("N / P keys", "Some web-based slide tools."),
    }

    @Composable
    fun presentationScreen(nav: NavHostController) {
        val session = LocalSession.current
        val connection by session.connection.collectAsState()
        val hapticFeedback = LocalHapticFeedback.current
        var advanceMode by remember { mutableStateOf(AdvanceMode.ARROWS) }
        var timerRunning by remember { mutableStateOf(false) }
        var elapsedMs by remember { mutableLongStateOf(0L) }
        KeepScreenOnWhileVisible()

        DisposableEffect(timerRunning) {
            var job: Job? = null
            if (timerRunning) {
                job = CoroutineScope(Dispatchers.Main).launch {
                    while (true) {
                        delay(1000)
                        elapsedMs += 1000
                    }
                }
            }
            onDispose { job?.cancel() }
        }

        fun tick() {
            if (hapticFeedback != null && false) Unit
        }

        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Presentation") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { HonestyChip(Honesty.CORE, Modifier.padding(end = 12.dp)) },
            )
            if (connection.connectionState != HidConnectionState.CONNECTED) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    StateCard(
                        title = "No computer connected",
                        body = "Slide keys reach the computer only while connected.",
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatTimer(elapsedMs),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallChip(
                            label = if (timerRunning) "Pause" else "Start timer",
                            active = timerRunning,
                        ) { timerRunning = !timerRunning }
                        SmallChip(label = "Reset", active = false) {
                            timerRunning = false
                            elapsedMs = 0
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    BigButton(
                        label = "Previous",
                        modifier = Modifier.weight(1f),
                    ) {
                        when (advanceMode) {
                            AdvanceMode.ARROWS -> session.tapKey(Keycodes.KEY_LEFT)
                            AdvanceMode.PAGE -> session.tapKey(Keycodes.KEY_PAGEUP)
                            AdvanceMode.NP -> session.tapKey(Keycodes.KEY_A + ('p' - 'a'))
                        }
                        if (hapticsActive()) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    BigButton(
                        label = "Next",
                        modifier = Modifier.weight(1.4f),
                        primary = true,
                    ) {
                        when (advanceMode) {
                            AdvanceMode.ARROWS -> session.tapKey(Keycodes.KEY_RIGHT)
                            AdvanceMode.PAGE -> session.tapKey(Keycodes.KEY_PAGEDOWN)
                            AdvanceMode.NP -> session.tapKey(Keycodes.KEY_A + ('n' - 'a'))
                        }
                        if (hapticsActive()) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AdvanceMode.entries.forEach { mode ->
                        SmallChip(label = mode.label, active = advanceMode == mode) { advanceMode = mode }
                    }
                }
                Text(
                    advanceMode.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SmallChip(label = "Start (F5)", active = false) {
                        session.tapKey(Keycodes.KEY_F5)
                    }
                    SmallChip(label = "End (Esc)", active = false) {
                        session.tapKey(Keycodes.KEY_ESC)
                    }
                    SmallChip(label = "Blank (B)", active = false) {
                        session.tapKey(Keycodes.KEY_A + ('b' - 'a'))
                    }
                }
                Text(
                    "Start, End and Blank send the F5, Esc and B keys. Exact behaviour depends on your presentation software — that is its own shortcut handling, not something Elevon can control.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    private fun hapticsActive(): Boolean = true

    private fun formatTimer(ms: Long): String {
        val totalSeconds = ms / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    // ---- shared buttons --------------------------------------------------------

    @Composable
    private fun MediaButton(
        icon: ImageVector,
        label: String,
        modifier: Modifier = Modifier,
        primary: Boolean = false,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = modifier
                .aspectRatio(1.1f)
                .border(
                    1.dp,
                    if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape,
                )
                .background(
                    if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    CircleShape,
                )
                .pointerInput(label) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onClick()
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) break
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }

    /** Button that repeats its action while held (volume, seek). */
    @Composable
    private fun HoldButton(
        label: String,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null,
        onAction: () -> Unit,
    ) {
        Box(
            modifier = modifier
                .height(64.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                .pointerInput(label) {
                    coroutineScope {
                        while (true) {
                            awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
                            onAction()
                            // Release watcher; the repeat runs while it tracks the hold.
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
                            delay(300) // initial repeat delay
                            val start = System.currentTimeMillis()
                            while (!released && System.currentTimeMillis() - start < 8000) {
                                onAction()
                                delay(200)
                            }
                            watcher.cancel()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(0.dp))
                    androidx.compose.foundation.layout.PaddingValues(0.dp).let { }
                }
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    @Composable
    private fun BigButton(
        label: String,
        modifier: Modifier = Modifier,
        primary: Boolean = false,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = modifier
                .border(
                    2.dp,
                    if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(24.dp),
                )
                .background(
                    if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(24.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.titleLarge)
        }
    }

    @Composable
    private fun SmallChip(label: String, active: Boolean, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .border(
                    1.dp,
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(10.dp),
                )
                .background(
                    if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(10.dp),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
