package app.elevon.input

import app.elevon.hid.Keycodes

/** The computer's operating system, chosen per profile or device. */
enum class HostOs(val label: String) {
    WINDOWS("Windows"),
    MACOS("macOS"),
    LINUX("Linux / SteamOS"),
    OTHER("Other");

    val isMac: Boolean get() = this == MACOS

    /** The primary shortcut modifier: Cmd on a Mac, Ctrl elsewhere. */
    val primary: Int get() = if (isMac) Keycodes.MOD_LGUI else Keycodes.MOD_LCTRL
}

data class Chord(val mods: Int, val usage: Int)

private fun key(c: Char): Int = Keycodes.KEY_A + (c.lowercaseChar() - 'a')

/**
 * OS-aware shortcuts. The same logical action maps to the combination the
 * connected computer actually understands — Ctrl+C on Windows and Linux,
 * Cmd+C on a Mac. Anything a browser/OS cannot deliver stays out of the map;
 * Elevon never fakes an action (see docs/research.md, honesty rules).
 */
object OsShortcuts {

    fun forOs(os: HostOs): Map<ShortcutId, Chord> {
        val m = os.primary
        val shift = Keycodes.MOD_LSHIFT
        return buildMap {
            put(ShortcutId.COPY, Chord(m, key('c')))
            put(ShortcutId.CUT, Chord(m, key('x')))
            put(ShortcutId.PASTE, Chord(m, key('v')))
            put(ShortcutId.UNDO, Chord(m, key('z')))
            put(ShortcutId.REDO, Chord(m or shift, if (os.isMac) key('z') else key('y')))
            put(ShortcutId.SELECT_ALL, Chord(m, key('a')))
            put(ShortcutId.FIND, Chord(m, key('f')))
            put(ShortcutId.SAVE, Chord(m, key('s')))
            put(
                ShortcutId.SWITCH_APP,
                if (os.isMac) Chord(m, Keycodes.KEY_TAB) else Chord(Keycodes.MOD_LALT, Keycodes.KEY_TAB),
            )
            put(
                ShortcutId.SHOW_DESKTOP,
                if (os.isMac) Chord(Keycodes.MOD_LFN, Keycodes.KEY_F11) else Chord(m, key('d')),
            )
            put(
                ShortcutId.SCREENSHOT,
                when {
                    os.isMac -> Chord(m or shift, Keycodes.KEY_1 + 2) // Cmd+Shift+3
                    os == HostOs.LINUX -> Chord(Keycodes.MOD_LCTRL, Keycodes.KEY_PRINTSCREEN)
                    else -> Chord(m, Keycodes.KEY_PRINTSCREEN) // Win+PrtSc
                },
            )
            put(
                ShortcutId.LOCK,
                if (os.isMac) Chord(Keycodes.MOD_LCTRL or Keycodes.MOD_LGUI, key('q')) else Chord(m, key('l')),
            )
            put(ShortcutId.CLOSE_TAB, Chord(m, key('w')))
            put(ShortcutId.NEW_TAB, Chord(m, key('t')))
            put(ShortcutId.REOPEN_TAB, Chord(m or shift, key('t')))
            put(
                ShortcutId.TASK_VIEW,
                if (os.isMac) Chord(Keycodes.MOD_LCTRL, Keycodes.KEY_UP) else Chord(m, Keycodes.KEY_TAB),
            )
        }
    }
}

enum class ShortcutId(val label: String) {
    COPY("Copy"),
    CUT("Cut"),
    PASTE("Paste"),
    UNDO("Undo"),
    REDO("Redo"),
    SELECT_ALL("Select all"),
    FIND("Find"),
    SAVE("Save"),
    SWITCH_APP("Switch app"),
    SHOW_DESKTOP("Show desktop"),
    SCREENSHOT("Screenshot"),
    LOCK("Lock"),
    CLOSE_TAB("Close tab"),
    NEW_TAB("New tab"),
    REOPEN_TAB("Reopen tab"),
    TASK_VIEW("Task view"),
}
