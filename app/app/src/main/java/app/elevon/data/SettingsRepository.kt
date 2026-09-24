package app.elevon.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

enum class ThemeMode(val label: String) { SYSTEM("System"), DARK("Graphite"), LIGHT("Paper") }
enum class HapticsMode(val label: String) { OFF("Off"), SUBTLE("Subtle"), FULL("Full") }
enum class HostLayout(val label: String, val note: String) {
    US("US (default)", "Standard US QWERTY — matches most computers."),
    UK("UK", "United Kingdom layout."),
    DE("German (QWERTZ)", "Y and Z swap; umlauts via AltGr are not typed over HID."),
    FR("French (AZERTY)", "A and Q swap; accented characters are not typed over HID."),
}

/** One observable preference: [value] is a StateFlow, [set] writes through. */
class StringPref(initial: String, private val onUpdate: (String) -> Unit) {
    private val flow = MutableStateFlow(initial)
    val value: StateFlow<String> = flow.asStateFlow()
    fun set(v: String) { flow.value = v; onUpdate(v) }
}

/** One observable boolean preference. */
class BoolPref(initial: Boolean, private val onUpdate: (Boolean) -> Unit) {
    private val flow = MutableStateFlow(initial)
    val value: StateFlow<Boolean> = flow.asStateFlow()
    fun set(v: Boolean) { flow.value = v; onUpdate(v) }
}

/** One observable float preference. */
class FloatPref(initial: Float, private val onUpdate: (Float) -> Unit) {
    private val flow = MutableStateFlow(initial)
    val value: StateFlow<Float> = flow.asStateFlow()
    fun set(v: Float) { flow.value = v; onUpdate(v) }
}

/**
 * All user preferences, backed by SharedPreferences. Small, synchronous and
 * observable — deliberately simple for an app with no backend.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("elevon_settings", Context.MODE_PRIVATE)

    private fun put(key: String, value: String) = prefs.edit().putString(key, value).apply()
    private fun put(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    private fun put(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    private fun put(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    // ---- appearance & feel -------------------------------------------------
    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(K_THEME, null) ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
    )
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()
    fun setTheme(mode: ThemeMode) { _theme.value = mode; put(K_THEME, mode.name) }

    private val _haptics = MutableStateFlow(
        runCatching { HapticsMode.valueOf(prefs.getString(K_HAPTICS, null) ?: "SUBTLE") }.getOrDefault(HapticsMode.SUBTLE),
    )
    val haptics: StateFlow<HapticsMode> = _haptics.asStateFlow()
    fun setHaptics(mode: HapticsMode) { _haptics.value = mode; put(K_HAPTICS, mode.name) }

    // ---- controls ----------------------------------------------------------
    val keyboardLayout = stringFlow(K_KB_LAYOUT, "COMPACT")
    val functionRow = boolFlow(K_FN_ROW, true)

    val pointerSpeed = floatFlow(K_POINTER, 1.0f)
    val scrollSpeed = floatFlow(K_SCROLL, 1.0f)
    val naturalScrolling = boolFlow(K_NATURAL, false)
    val tapToClick = boolFlow(K_TAP_CLICK, true)
    val dragLock = boolFlow(K_DRAG_LOCK, true)

    val keepScreenOn = boolFlow(K_KEEP_ON, true)

    // ---- keyboard (host side) ----------------------------------------------
    val hostLayout = stringFlow(K_HOST_LAYOUT, HostLayout.US.name)
    val customKeyboard = stringFlow("custom_keyboard", "")

    // ---- privacy / clipboard ------------------------------------------------
    val clipboardRetention = boolFlow(K_CLIP_RETAIN, true)

    private val _clipboardHistory = MutableStateFlow(loadClipboardHistory())
    val clipboardHistory: StateFlow<List<String>> = _clipboardHistory.asStateFlow()

    fun rememberClipboard(text: String) {
        if (!clipboardRetention.value.value || text.isBlank()) return
        val next = (listOf(text) + _clipboardHistory.value).distinct().take(MAX_CLIPBOARD)
        _clipboardHistory.value = next
        put(K_CLIP_HISTORY, JSONArray(next).toString())
    }

    fun clearClipboardHistory() {
        _clipboardHistory.value = emptyList()
        put(K_CLIP_HISTORY, "[]")
    }

    fun setClipboardHistory(items: List<String>) {
        val next = items.distinct().take(MAX_CLIPBOARD)
        _clipboardHistory.value = next
        put(K_CLIP_HISTORY, JSONArray(next).toString())
    }

    private fun loadClipboardHistory(): List<String> = runCatching {
        val arr = JSONArray(prefs.getString(K_CLIP_HISTORY, "[]") ?: "[]")
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    // ---- onboarding ---------------------------------------------------------
    private val _onboarded = MutableStateFlow(prefs.getBoolean(K_ONBOARDED, false))
    val onboarded: StateFlow<Boolean> = _onboarded.asStateFlow()
    fun setOnboarded() { _onboarded.value = true; put(K_ONBOARDED, true) }

    // ---- helpers ------------------------------------------------------------

    private fun stringFlow(key: String, def: String) =
        StringPref(prefs.getString(key, def) ?: def) { put(key, it) }

    private fun boolFlow(key: String, def: Boolean) =
        BoolPref(prefs.getBoolean(key, def)) { put(key, it) }

    private fun floatFlow(key: String, def: Float) =
        FloatPref(prefs.getFloat(key, def)) { put(key, it) }
    }

    private companion object {
        const val K_THEME = "theme"
        const val K_HAPTICS = "haptics"
        const val K_KB_LAYOUT = "kb_layout"
        const val K_FN_ROW = "kb_fn_row"
        const val K_POINTER = "pointer_speed"
        const val K_SCROLL = "scroll_speed"
        const val K_NATURAL = "natural_scrolling"
        const val K_TAP_CLICK = "tap_to_click"
        const val K_DRAG_LOCK = "drag_lock"
        const val K_KEEP_ON = "keep_screen_on"
        const val K_HOST_LAYOUT = "host_layout"
        const val K_CUSTOM_KB = "custom_keyboard"
        const val K_CLIP_RETAIN = "clipboard_retention"
        const val K_CLIP_HISTORY = "clipboard_history"
        const val K_ONBOARDED = "onboarded"
        const val MAX_CLIPBOARD = 20
    }
}
