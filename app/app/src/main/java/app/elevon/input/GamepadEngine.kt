package app.elevon.input

import app.elevon.hid.GamepadReport
import app.elevon.hid.HidController
import app.elevon.hid.Keycodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Live gamepad state driven by the Gamepad screen, streamed to the host at a
 * fixed rate. In GAMEPAD mode it packs a native HID gamepad report; in
 * KEYBOARD_MOUSE mode the same on-screen controls emit keys and mouse deltas,
 * which is how Elevon supports games with no controller support at all
 * (see docs/compatibility.md, "Gamepad output modes").
 */
class GamepadEngine(private val hid: HidController) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    /** Analogue stick positions, -1..1 (x right, y down). */
    data class Sticks(val lx: Float = 0f, val ly: Float = 0f, val rx: Float = 0f, val ry: Float = 0f)

    data class PadState(
        val buttons: Set<Int> = emptySet(),   // Keycodes.BTN_*
        val dpad: Set<Int> = emptySet(),      // 0=up 1=right 2=down 3=left
        val triggers: Pair<Float, Float> = 0f to 0f, // LT, RT, 0..1
        val sticks: Sticks = Sticks(),
    )

    private val _state = MutableStateFlow(PadState())
    val state: StateFlow<PadState> = _state.asStateFlow()

    private val heldKeys = mutableSetOf<Int>()
    private val heldMods = mutableSetOf<Int>()

    private var profile: GamepadProfile = GamepadProfiles.standard()

    fun setProfile(p: GamepadProfile) {
        profile = p
        releaseAllKeyboard()
    }

    fun start() {
        if (loop?.isActive == true) return
        releaseAllKeyboard()
        loop = scope.launch {
            var lastSent = 0L
            while (isActive) {
                val now = System.currentTimeMillis()
                tick()
                delay(TICK_MS)
                lastSent = now
            }
        }
    }

    fun stop() {
        loop?.cancel()
        loop = null
        releaseAllKeyboard()
        hid.gamepad.reset()
        _state.value = PadState()
    }

    // ---- UI entry points ----------------------------------------------------

    /** Presses an on-screen control by element id; routes by output type. */
    fun pressElement(elementId: String, down: Boolean) {
        val element = profile.element(elementId) ?: return
        when (val output = element.output) {
            is PadOutput.PadButton -> setButton(output.bit, down)
            is PadOutput.Chords, is PadOutput.MouseClick -> applyDigitalOutput(element, down)
            else -> Unit
        }
    }

    fun setButton(bit: Int, down: Boolean) {
        val b = _state.value.buttons.toMutableSet()
        if (down) b.add(bit) else b.remove(bit)
        _state.value = _state.value.copy(buttons = b)
        if (profile.outputMode == GamepadProfile.OutputMode.GAMEPAD) return
        // Keyboard/mouse mode: find the element that emits this native button.
        val element = profile.elements.firstOrNull {
            (it.output as? PadOutput.PadButton)?.bit == bit
        } ?: return
        applyDigitalOutput(element, down)
    }

    fun setDpad(dirs: Set<Int>) {
        val old = _state.value.dpad
        _state.value = _state.value.copy(dpad = dirs)
        handleHatEdges(old, dirs)
    }

    fun setTrigger(index: Int, value: Float) {
        val t = if (index == 0) {
            _state.value.triggers.copy(first = value)
        } else {
            _state.value.triggers.copy(second = value)
        }
        _state.value = _state.value.copy(triggers = t)
    }

    fun setStick(stick: Int, x: Float, y: Float) {
        val s = _state.value.sticks
        _state.value = _state.value.copy(
            sticks = when (stick) {
                0 -> s.copy(lx = x, ly = y)
                else -> s.copy(rx = x, ry = y)
            },
        )
    }

    // ---- internals ----------------------------------------------------------

    private fun tick() {
        val s = _state.value
        when (profile.outputMode) {
            GamepadProfile.OutputMode.GAMEPAD -> {
                val dz = profile.deadZone
                val (lx, ly) = GamepadReport.applyDeadZone(s.sticks.lx, s.sticks.ly, dz)
                val (rx, ry) = GamepadReport.applyDeadZone(s.sticks.rx, s.sticks.ry, dz)
                hid.gamepad.setSticks(
                    GamepadReport.clampSigned((lx * 127).toInt()),
                    GamepadReport.clampSigned((ly * 127).toInt()),
                    GamepadReport.clampSigned((rx * 127).toInt()),
                    GamepadReport.clampSigned((ry * 127).toInt()),
                )
                hid.gamepad.setTriggers(
                    GamepadReport.clampUnsigned((s.triggers.first * 255).toInt()),
                    GamepadReport.clampUnsigned((s.triggers.second * 255).toInt()),
                )
                var buttons = 0
                for (b in s.buttons) buttons = buttons or (1 shl b)
                // L3/R3: deep-press zones encoded as stick clicks when triggered via UI
                hid.gamepad.apply {
                    for (bit in 0..10) setButton(bit, (buttons shr bit) and 1 == 1)
                    setHat(GamepadReport.hatFor(
                        up = 0 in s.dpad, right = 1 in s.dpad,
                        down = 2 in s.dpad, left = 3 in s.dpad,
                    ))
                }
                hid.sendGamepad(hid.gamepad.payload())
            }

            GamepadProfile.OutputMode.KEYBOARD_MOUSE -> {
                // Movement stick -> held WASD-style keys (already handled by edges
                // in setStick via digital thresholds); here only MouseLook streams.
                val (rx, ry) = GamepadReport.applyDeadZone(s.sticks.rx, s.sticks.ry, profile.deadZone)
                if (rx != 0f || ry != 0f) {
                    val scale = 14f * profile.sensitivity
                    hid.mouse.move((rx * scale).toInt(), (ry * scale).toInt())
                    hid.sendMouseState()
                }
            }
        }
    }

    private fun handleButtonEdges(bit: Int, down: Boolean) {
        if (profile.outputMode == GamepadProfile.OutputMode.GAMEPAD) return
        val element = profile.elements.firstOrNull {
            it.output is PadOutput.PadButton && it.output.bit == bit
        } ?: return
        applyDigitalOutput(element, down)
    }

    private fun handleHatEdges(old: Set<Int>, new: Set<Int>) {
        if (profile.outputMode == GamepadProfile.OutputMode.GAMEPAD) return
        val element = profile.elements.firstOrNull { it.kind == PadKind.DPAD } ?: return
        val chords = (element.output as? PadOutput.Chords)?.chords ?: return
        // chords order: up, right, down, left
        for (dir in 0..3) {
            val chord = chords.getOrNull(dir) ?: continue
            val usage = chord.usage
            if (dir in new && dir !in old) {
                if (usage != 0) heldKeys.add(usage)
                flushChord(chord.mods)
            } else if (dir !in new && dir in old) {
                heldKeys.remove(usage)
                flushChord(chord.mods)
            }
        }
    }

    /** Called by the UI as the stick crosses digital thresholds (keyboard mode). */
    fun keyboardStickDirs(stick: Int, dirs: Set<Int>, chords: List<Chord>) {
        if (stick != 0) return
        val key = "stick$stick"
        val previous = digitalStickState[key] ?: emptySet()
        digitalStickState[key] = dirs
        for (dir in 0..3) {
            val chord = chords.getOrNull(dir) ?: continue
            if (dir in dirs && dir !in previous) {
                if (chord.usage != 0) heldKeys.add(chord.usage)
                heldMods.add(chord.mods)
            } else if (dir !in dirs && dir in previous) {
                heldKeys.remove(chord.usage)
                if (heldKeys.none { it == chord.usage }) heldMods.remove(chord.mods)
            }
        }
        flushChord(0)
    }

    private val digitalStickState = mutableMapOf<String, Set<Int>>()

    private fun applyDigitalOutput(element: PadElement, down: Boolean) {
        when (val o = element.output) {
            is PadOutput.Chords -> {
                val chord = o.chords.firstOrNull() ?: return
                if (down) {
                    if (chord.usage != 0) heldKeys.add(chord.usage)
                    if (chord.mods != 0) heldMods.add(chord.mods)
                } else {
                    heldKeys.remove(chord.usage)
                    heldMods.remove(chord.mods)
                }
                flushChord(0)
            }
            is PadOutput.MouseClick -> {
                hid.mouse.setButton(o.button, down)
                hid.sendMouseState()
            }
            else -> Unit
        }
    }

    private fun flushChord(extraMods: Int) {
        val mods = (heldMods.fold(0) { acc, m -> acc or m }) or extraMods
        hid.sendRawKeyboard(mods and 0xFF, heldKeys.toList())
    }

    private fun releaseAllKeyboard() {
        heldKeys.clear()
        heldMods.clear()
        digitalStickState.clear()
        hid.sendRawKeyboard(0, emptyList())
    }

    companion object {
        const val TICK_MS = 16L
    }
}
