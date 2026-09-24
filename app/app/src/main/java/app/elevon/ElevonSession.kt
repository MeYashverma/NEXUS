package app.elevon

import android.content.Context
import app.elevon.data.ControlMode
import app.elevon.data.Profile
import app.elevon.data.Repositories
import app.elevon.data.SavedDevice
import app.elevon.data.SettingsRepository
import app.elevon.hid.HidConnectionState
import app.elevon.hid.HidController
import app.elevon.hid.HidUiState
import app.elevon.hid.Keycodes
import app.elevon.hid.MouseReport
import app.elevon.input.Chord
import app.elevon.input.GamepadEngine
import app.elevon.input.GamepadProfiles
import app.elevon.input.HostOs
import app.elevon.input.MacroStep
import app.elevon.input.OsShortcuts
import app.elevon.input.ShortcutId
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

/** Which macro/deck action is running, for progress UI. */
data class MacroRun(
    val label: String,
    val step: Int,
    val totalSteps: Int,
)

/**
 * The application facade: one object the whole UI talks to. Owns the HID
 * controller, repositories, gamepad engine and macro runner, and applies the
 * active profile to whatever the user opens.
 */
class ElevonSession(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val hid: HidController = HidController(context)
    val settings = SettingsRepository(context)
    val repos = Repositories(context)
    val gamepad = GamepadEngine(hid)

    private val _activeMode = MutableStateFlow<ControlMode?>(null)
    val activeMode: StateFlow<ControlMode?> = _activeMode.asStateFlow()

    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    private val _macroRun = MutableStateFlow<MacroRun?>(null)
    val macroRun: StateFlow<MacroRun?> = _macroRun.asStateFlow()

    private var currentHostAddress: String? = null
    private var macroJob: Job? = null

    val connection: StateFlow<HidUiState> get() = hid.state

    /** The OS-aware shortcut set for the connected host (or active profile). */
    fun shortcuts(): Map<ShortcutId, Chord> =
        OsShortcuts.forOs(hostOs())

    fun hostOs(): HostOs {
        val device = currentHostAddress?.let { addr ->
            repos.devices.value.firstOrNull { it.address == addr }
        }
        return activeProfile.value?.hostOs
            ?: device?.hostOs
            ?: HostOs.WINDOWS
    }

    // ---- connection ---------------------------------------------------------

    fun connect(address: String, name: String) {
        currentHostAddress = address
        val device = repos.rememberDevice(address, name)
        val preferred = repos.profile(device.preferredProfileId)
        applyProfile(preferred)
        hid.connect(address)
        observeConnection()
    }

    fun disconnect() {
        hid.disconnect()
    }

    private var connectionObserver: Job? = null

    private fun observeConnection() {
        if (connectionObserver != null) return
        connectionObserver = scope.launch {
            hid.state.collect { state ->
                when (state.connectionState) {
                    HidConnectionState.CONNECTED -> {
                        val host = state.hostName ?: "computer"
                        app.elevon.hid.ElevonHidService.start(context, host)
                    }
                    else -> app.elevon.hid.ElevonHidService.stop(context)
                }
            }
        }
    }

    // ---- profiles -----------------------------------------------------------

    fun applyProfile(profile: Profile?) {
        _activeProfile.value = profile
        _activeMode.value = profile?.mode
        val pad = if (profile?.gamepadProfileId.isNullOrBlank()) {
            GamepadProfiles.standard()
        } else {
            GamepadProfiles.defaults().firstOrNull { it.id == profile!!.gamepadProfileId }
                ?: GamepadProfiles.standard()
        }
        gamepad.setProfile(pad)
    }

    fun setPreferredProfileForCurrentDevice(profileId: String?) {
        val addr = currentHostAddress ?: return
        val device = repos.devices.value.firstOrNull { it.address == addr } ?: return
        repos.updateDevice(device.copy(preferredProfileId = profileId))
    }

    // ---- shared input actions ----------------------------------------------

    fun sendShortcut(id: ShortcutId) {
        val chord = shortcuts()[id] ?: return
        tapKey(chord.usage, chord.mods)
    }

    /** Press and release a single key (optionally with modifiers). */
    fun tapKey(usage: Int, mods: Int = 0) {
        hid.sendRawKeyboard(mods and 0xFF, if (usage == 0) emptyList() else listOf(usage))
        scope.launch {
            delay(12)
            hid.sendRawKeyboard(0, emptyList())
        }
    }

    fun sendConsumer(usage: Int) = hid.sendConsumer(usage)

    fun sendSystem(code: Int) = hid.sendSystem(code)

    fun typeText(text: String) {
        hid.typeText(text)
        settings.rememberClipboard(text)
    }

    /** "Send clipboard to computer": Elevon types it as keystrokes. */
    fun sendClipboardToComputer(text: String, onProgress: (Float) -> Unit, onDone: (Boolean) -> Unit) {
        hid.typeText(text, onProgress, onDone)
    }

    // ---- macros & decks ------------------------------------------------------

    fun runMacro(label: String, steps: List<MacroStep>) {
        macroJob?.cancel()
        macroJob = scope.launch {
            val total = steps.size
            _macroRun.value = MacroRun(label, 0, total)
            try {
                for ((index, step) in steps.withIndex()) {
                    if (!isActive) return@launch
                    _macroRun.value = MacroRun(label, index + 1, total)
                    when (step) {
                        is MacroStep.Chord -> {
                            hid.sendRawKeyboard(step.mods and 0xFF, if (step.usage == 0) emptyList() else listOf(step.usage))
                            delay(14)
                            hid.sendRawKeyboard(0, emptyList())
                            delay(24)
                        }
                        is MacroStep.Text -> {
                            hid.typeText(step.text)
                            // typeText runs on its own job; wait for completion heuristically
                            delay(step.text.length.coerceAtMost(600) * 12L + 80L)
                        }
                        is MacroStep.Media -> {
                            sendConsumer(step.usage)
                            delay(60)
                        }
                        is MacroStep.Delay -> delay(step.ms)
                    }
                }
            } finally {
                _macroRun.value = null
            }
        }
    }

    fun cancelMacro() {
        macroJob?.cancel()
        hid.cancelTyping()
        _macroRun.value = null
    }

    // ---- mouse helpers for the touchpad --------------------------------------

    fun mouseMove(dx: Float, dy: Float, sensitivity: Float) {
        hid.warmUpIfIdle()
        hid.mouse.move(
            (dx * 0.16f * sensitivity).toInt().coerceIn(-127, 127),
            (dy * 0.16f * sensitivity).toInt().coerceIn(-127, 127),
        )
        hid.sendMouseState()
    }

    fun mouseButton(button: Int, down: Boolean) {
        hid.warmUpIfIdle()
        hid.mouse.setButton(button, down)
        hid.sendMouseState()
    }

    fun scroll(notches: Float, natural: Boolean, speed: Float) {
        hid.warmUpIfIdle()
        val direction = if (natural) -1 else 1
        hid.mouse.scroll((notches * speed * direction).toInt().coerceIn(-30, 30))
        hid.sendMouseState()
    }

    fun pan(notches: Float, speed: Float) {
        hid.warmUpIfIdle()
        hid.mouse.panHorizontal((notches * speed).toInt().coerceIn(-30, 30))
        hid.sendMouseState()
    }

    fun capsLockToggle() {
        hid.sendRawKeyboard(0, listOf(Keycodes.KEY_CAPSLOCK))
        scope.launch {
            delay(14)
            hid.sendRawKeyboard(0, emptyList())
        }
    }
}
