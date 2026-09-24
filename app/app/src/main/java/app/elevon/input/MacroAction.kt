package app.elevon.input

import app.elevon.hid.Keycodes
import org.json.JSONArray
import org.json.JSONObject

/** One step inside a macro. */
sealed class MacroStep {
    /** Press and release a chord (mods held, key tapped). */
    data class Chord(val mods: Int, val usage: Int) : MacroStep()

    /** Type text (US layout over HID). */
    data class Text(val text: String) : MacroStep()

    /** Consumer-page media/system key. */
    data class Media(val usage: Int) : MacroStep()

    /** Wait, in milliseconds. */
    data class Delay(val ms: Long) : MacroStep()

    fun toJson(): JSONObject {
        val o = JSONObject()
        when (this) {
            is Chord -> {
                o.put("t", "chord"); o.put("mods", mods); o.put("usage", usage)
            }
            is Text -> {
                o.put("t", "text"); o.put("text", text)
            }
            is Media -> {
                o.put("t", "media"); o.put("usage", usage)
            }
            is Delay -> {
                o.put("t", "delay"); o.put("ms", ms)
            }
        }
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): MacroStep = when (o.optString("t")) {
            "text" -> Text(o.optString("text"))
            "media" -> Media(o.getInt("usage"))
            "delay" -> Delay(o.getLong("ms"))
            else -> Chord(o.optInt("mods", 0), o.optInt("usage", 0))
        }
    }
}

/** Human-readable chord, e.g. "Ctrl+Shift+Esc" or "Cmd+key". */
fun Chord.describe(): String = buildString {
    if (mods and Keycodes.MOD_LGUI != 0) append("Cmd")
    if (mods and Keycodes.MOD_LCTRL != 0) append(if (isEmpty()) "Ctrl" else "+Ctrl")
    if (mods and Keycodes.MOD_LSHIFT != 0) append(if (isEmpty()) "Shift" else "+Shift")
    if (mods and Keycodes.MOD_LALT != 0) append(if (isEmpty()) "Alt" else "+Alt")
    if (usage != 0) append(if (isEmpty()) "key" else "+key")
}

/** A big deck button: label, optional subtitle, and its steps. */
data class MacroAction(
    val id: String,
    val label: String,
    val subtitle: String = "",
    val steps: List<MacroStep>,
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("label", label)
        o.put("subtitle", subtitle)
        val arr = JSONArray()
        steps.forEach { arr.put(it.toJson()) }
        o.put("steps", arr)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): MacroAction {
            val arr = o.optJSONArray("steps") ?: JSONArray()
            val steps = ArrayList<MacroStep>(arr.length())
            for (i in 0 until arr.length()) steps.add(MacroStep.fromJson(arr.getJSONObject(i)))
            return MacroAction(
                id = o.optString("id"),
                label = o.optString("label"),
                subtitle = o.optString("subtitle"),
                steps = steps,
            )
        }
    }
}

/** A page of the macro pad / custom surface (12 buttons in a 3×4 grid). */
data class MacroPage(
    val id: String,
    val name: String,
    val buttons: List<MacroAction?>, // length 12, null = empty slot
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("name", name)
        val arr = JSONArray()
        buttons.forEach { arr.put(it?.toJson() ?: JSONObject.NULL) }
        root.put("buttons", arr)
        return root.toString()
    }

    companion object {
        const val SLOTS = 12

        fun fromJson(text: String): MacroPage? = runCatching {
            val root = JSONObject(text)
            val arr = root.getJSONArray("buttons")
            val buttons = arrayOfNulls<MacroAction>(SLOTS)
            for (i in 0 until minOf(SLOTS, arr.length())) {
                if (!arr.isNull(i)) buttons[i] = MacroAction.fromJson(arr.getJSONObject(i))
            }
            MacroPage(root.getString("id"), root.getString("name"), buttons.toList())
        }.getOrNull()
    }
}

/**
 * Starter decks. Every button runs over standard keyboard and media keys, so
 * no host software is needed. This is the point: deck apps that require a
 * desktop server (Stream Deck Mobile, Macro Deck) are the competition, and
 * Elevon's decks work through plain HID.
 */
object MacroPresets {

    private fun action(id: String, label: String, subtitle: String, chord: Chord) =
        MacroAction(id, label, subtitle, listOf(MacroStep.Chord(chord.mods, chord.usage)))

    fun productivity(os: HostOs): List<MacroAction?> {
        val s = OsShortcuts.forOs(os)
        fun sc(id: ShortcutId) = action(
            id.name.lowercase(), id.label, s[id]!!.describe(), s[id]!!,
        )
        return listOf(
            sc(ShortcutId.COPY),
            sc(ShortcutId.PASTE),
            sc(ShortcutId.CUT),
            sc(ShortcutId.UNDO),
            sc(ShortcutId.REDO),
            sc(ShortcutId.SCREENSHOT),
            sc(ShortcutId.SWITCH_APP),
            sc(ShortcutId.SHOW_DESKTOP),
            sc(ShortcutId.LOCK),
            sc(ShortcutId.SELECT_ALL),
            sc(ShortcutId.SAVE),
            sc(ShortcutId.FIND),
        )
    }

    fun media(): List<MacroAction?> = listOf(
        MacroAction("play", "Play", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_PLAY_PAUSE))),
        MacroAction("next", "Next", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_SCAN_NEXT))),
        MacroAction("prev", "Previous", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_SCAN_PREVIOUS))),
        MacroAction("volup", "Vol +", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_VOLUME_UP))),
        MacroAction("voldn", "Vol −", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_VOLUME_DOWN))),
        MacroAction("mute", "Mute", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_MUTE))),
        MacroAction("stop", "Stop", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_STOP))),
        MacroAction("ff", "Forward", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_FAST_FORWARD))),
        MacroAction("rw", "Rewind", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_REWIND))),
        MacroAction("brightup", "Bright +", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_BRIGHTNESS_UP))),
        MacroAction("brightdn", "Bright −", "Media key", listOf(MacroStep.Media(Keycodes.CONSUMER_BRIGHTNESS_DOWN))),
        null,
    )

    fun snippets(): List<MacroAction?> = listOf(
        MacroAction("email", "My email", "Types text", listOf(MacroStep.Text("you@example.com\n"))),
        MacroAction("url", "Project URL", "Types text", listOf(MacroStep.Text("https://github.com/\n"))),
        MacroAction("sig", "Signature", "Types text", listOf(MacroStep.Text("Best regards,\n")),
        ),
        null, null, null, null, null, null, null, null, null,
    )
}
