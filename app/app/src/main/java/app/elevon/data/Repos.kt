package app.elevon.data

import android.content.Context
import app.elevon.input.HostOs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Primary control modes — now 9 with numpad + gyro. */
enum class ControlMode(val label: String, val description: String) {
    KEYBOARD("Keyboard", "Full, compact and gaming layouts"),
    TOUCHPAD("Touchpad", "Move, click, scroll, drag"),
    GAMEPAD("Gamepad", "Sticks, buttons, triggers, profiles — fullscreen landscape"),
    MACROS("Macro Pad", "Pages of one-tap actions"),
    MEDIA("Media", "Play, volume, seek"),
    PRESENTATION("Presentation", "Slides, blank screen, timer"),
    CUSTOM("Custom", "Build your own control surface"),
    NUMPAD("Numpad", "Numeric keypad + calculations — fullscreen"),
    GYRO("Gyro Mouse", "Air mouse via gyroscope — Labs, experimental"),
}

/** A computer the user has connected to before. */
data class SavedDevice(
    val address: String,
    val name: String,
    val firstSeen: Long,
    val lastConnected: Long,
    val preferredProfileId: String?,
    val hostOs: HostOs,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("address", address)
        .put("name", name)
        .put("firstSeen", firstSeen)
        .put("lastConnected", lastConnected)
        .put("profile", preferredProfileId ?: JSONObject.NULL)
        .put("os", hostOs.name)

    companion object {
        fun fromJson(o: JSONObject) = SavedDevice(
            address = o.getString("address"),
            name = o.getString("name"),
            firstSeen = o.optLong("firstSeen", 0L),
            lastConnected = o.optLong("lastConnected", 0L),
            preferredProfileId = if (o.isNull("profile")) null else o.optString("profile"),
            hostOs = runCatching { HostOs.valueOf(o.optString("os")) }.getOrDefault(HostOs.WINDOWS),
        )
    }
}

/** A named bundle of "how I control this computer". */
data class Profile(
    val id: String,
    val name: String,
    val mode: ControlMode,
    val hostOs: HostOs,
    val keyboardLayout: String,   // KeyboardLayoutId name
    val gamepadProfileId: String, // "" = Standard
    val macroPageName: String,    // "" = first page
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("mode", mode.name)
        .put("os", hostOs.name)
        .put("kb", keyboardLayout)
        .put("pad", gamepadProfileId)
        .put("deck", macroPageName)

    companion object {
        fun fromJson(o: JSONObject) = Profile(
            id = o.getString("id"),
            name = o.getString("name"),
            mode = runCatching { ControlMode.valueOf(o.getString("mode")) }.getOrDefault(ControlMode.TOUCHPAD),
            hostOs = runCatching { HostOs.valueOf(o.optString("os")) }.getOrDefault(HostOs.WINDOWS),
            keyboardLayout = o.optString("kb", "COMPACT"),
            gamepadProfileId = o.optString("pad", ""),
            macroPageName = o.optString("deck", ""),
        )

        fun defaults(): List<Profile> = listOf(
            Profile("p_work", "Work Laptop", ControlMode.TOUCHPAD, HostOs.WINDOWS, "COMPACT", "", "Productivity"),
            Profile("p_gaming", "Gaming PC", ControlMode.GAMEPAD, HostOs.WINDOWS, "GAMING", "std", ""),
            Profile("p_living", "Living Room PC", ControlMode.MEDIA, HostOs.OTHER, "COMPACT", "", "Media"),
            Profile("p_present", "Presentation", ControlMode.PRESENTATION, HostOs.OTHER, "COMPACT", "", ""),
            Profile("p_mac", "Mac", ControlMode.TOUCHPAD, HostOs.MACOS, "FULL", "", "Productivity"),
        )
    }
}

/**
 * Saved devices and profiles, persisted as JSON in SharedPreferences.
 * Nothing here ever leaves the phone (the app has no INTERNET permission).
 */
class Repositories(context: Context) {

    private val prefs = context.getSharedPreferences("elevon_store", Context.MODE_PRIVATE)

    // ---- devices ------------------------------------------------------------

    private val _devices = MutableStateFlow(loadDevices())
    val devices: StateFlow<List<SavedDevice>> = _devices.asStateFlow()

    fun rememberDevice(address: String, name: String): SavedDevice {
        val now = System.currentTimeMillis()
        val existing = _devices.value.firstOrNull { it.address == address }
        val device = existing?.copy(name = name, lastConnected = now)
            ?: SavedDevice(address, name, now, now, null, HostOs.WINDOWS)
        val next = listOf(device) + _devices.value.filterNot { it.address == address }
        _devices.value = next
        persistDevices()
        return device
    }

    fun updateDevice(device: SavedDevice) {
        _devices.value = _devices.value.map { if (it.address == device.address) device else it }
        persistDevices()
    }

    fun forgetDevice(address: String) {
        _devices.value = _devices.value.filterNot { it.address == address }
        persistDevices()
    }

    private fun persistDevices() {
        val arr = JSONArray()
        _devices.value.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(K_DEVICES, arr.toString()).apply()
    }

    private fun loadDevices(): List<SavedDevice> = runCatching {
        val arr = JSONArray(prefs.getString(K_DEVICES, "[]") ?: "[]")
        (0 until arr.length()).map { SavedDevice.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    // ---- profiles -----------------------------------------------------------

    private val _profiles = MutableStateFlow(loadProfiles())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    fun upsertProfile(profile: Profile) {
        val next = _profiles.value.filterNot { it.id == profile.id } + profile
        _profiles.value = next.sortedBy { it.name }
        persistProfiles()
    }

    fun deleteProfile(id: String) {
        _profiles.value = _profiles.value.filterNot { it.id == id }
        persistProfiles()
    }

    fun profile(id: String?): Profile? = _profiles.value.firstOrNull { it.id == id }

    private fun persistProfiles() {
        val arr = JSONArray()
        _profiles.value.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(K_PROFILES, arr.toString()).apply()
    }

    private fun loadProfiles(): List<Profile> {
        val text = prefs.getString(K_PROFILES, null) ?: return Profile.defaults()
        return runCatching {
            val arr = JSONArray(text)
            if (arr.length() == 0) return Profile.defaults()
            (0 until arr.length()).map { Profile.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(Profile.defaults())
    }

    private companion object {
        const val K_DEVICES = "devices"
        const val K_PROFILES = "profiles"
        const val K_PADS = "pads"
        const val K_DECKS = "decks"
    }

    // ---- gamepad profiles (user-edited) -------------------------------------

    private val _pads = MutableStateFlow(loadPads())
    val pads: StateFlow<List<app.elevon.input.GamepadProfile>> = _pads.asStateFlow()

    fun upsertPad(pad: app.elevon.input.GamepadProfile) {
        _pads.value = listOf(pad) + _pads.value.filterNot { it.id == pad.id }
        val arr = JSONArray()
        _pads.value.forEach { arr.put(JSONObject(it.toJson())) }
        prefs.edit().putString(K_PADS, arr.toString()).apply()
    }

    fun deletePad(id: String) {
        _pads.value = _pads.value.filterNot { it.id == id }
        prefs.edit().putString(K_PADS, JSONArray().toString()).apply()
    }

    private fun loadPads(): List<app.elevon.input.GamepadProfile> = runCatching {
        val arr = JSONArray(prefs.getString(K_PADS, "[]") ?: "[]")
        (0 until arr.length()).mapNotNull {
            app.elevon.input.GamepadProfile.fromJson(arr.getJSONObject(it).toString())
        }
    }.getOrDefault(emptyList())

    // ---- macro pages / decks ---------------------------------------------------

    private val _decks = MutableStateFlow(loadDecks())
    val decks: StateFlow<List<app.elevon.input.MacroPage>> = _decks.asStateFlow()

    fun upsertDeck(page: app.elevon.input.MacroPage) {
        _decks.value = listOf(page) + _decks.value.filterNot { it.id == page.id }
        val arr = JSONArray()
        _decks.value.forEach { arr.put(JSONObject(it.toJson())) }
        prefs.edit().putString(K_DECKS, arr.toString()).apply()
    }

    fun deleteDeck(id: String) {
        _decks.value = _decks.value.filterNot { it.id == id }
        prefs.edit().putString(K_DECKS, JSONArray().toString()).apply()
    }

    private fun loadDecks(): List<app.elevon.input.MacroPage> = runCatching {
        val arr = JSONArray(prefs.getString(K_DECKS, "[]") ?: "[]")
        (0 until arr.length()).mapNotNull {
            app.elevon.input.MacroPage.fromJson(arr.getJSONObject(it).toString())
        }
    }.getOrDefault(emptyList())
}
