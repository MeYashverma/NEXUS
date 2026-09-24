package app.elevon.hid

/**
 * Maps characters to HID keystrokes for the **US QWERTY** layout — the layout
 * every OS ships by default. If the computer is set to another layout, users
 * pick it in Elevon's per-profile "host keyboard layout" setting, which swaps
 * this table's symbol placements (see docs/compatibility.md).
 *
 * The receiver (Relay/IME) side is layout-independent because it commits text;
 * only typed-over-HID text needs this table.
 */
object CharMap {

    /** (usage, requiresShift) for a character, or null when unsupported. */
    fun keystrokeFor(c: Char): Pair<Int, Boolean>? {
        Keycodes.keyForLetter(c)?.let { return it to false }
        Keycodes.keyForLetter(c.lowercaseChar())?.let { return it to true }
        return when (c) {
            '1' -> Keycodes.KEY_1 to false; '!' -> Keycodes.KEY_1 to true
            '2' -> Keycodes.KEY_2 to false; '@' -> Keycodes.KEY_2 to true
            '3' -> Keycodes.KEY_3 to false; '#' -> Keycodes.KEY_3 to true
            '4' -> Keycodes.KEY_4 to false; '$' -> Keycodes.KEY_4 to true
            '5' -> Keycodes.KEY_5 to false; '%' -> Keycodes.KEY_5 to true
            '6' -> Keycodes.KEY_6 to false; '^' -> Keycodes.KEY_6 to true
            '7' -> Keycodes.KEY_7 to false; '&' -> Keycodes.KEY_7 to true
            '8' -> Keycodes.KEY_8 to false; '*' -> Keycodes.KEY_8 to true
            '9' -> Keycodes.KEY_9 to false; '(' -> Keycodes.KEY_9 to true
            '0' -> Keycodes.KEY_0 to false; ')' -> Keycodes.KEY_0 to true
            ' ' -> Keycodes.KEY_SPACE to false
            '\n' -> Keycodes.KEY_ENTER to false
            '\t' -> Keycodes.KEY_TAB to false
            '-' -> Keycodes.KEY_MINUS to false; '_' -> Keycodes.KEY_MINUS to true
            '=' -> Keycodes.KEY_EQUAL to false; '+' -> Keycodes.KEY_EQUAL to true
            '[' -> Keycodes.KEY_LEFTBRACKET to false; '{' -> Keycodes.KEY_LEFTBRACKET to true
            ']' -> Keycodes.KEY_RIGHTBRACKET to false; '}' -> Keycodes.KEY_RIGHTBRACKET to true
            '\\' -> Keycodes.KEY_BACKSLASH to false; '|' -> Keycodes.KEY_BACKSLASH to true
            ';' -> Keycodes.KEY_SEMICOLON to false; ':' -> Keycodes.KEY_SEMICOLON to true
            '\'' -> Keycodes.KEY_APOSTROPHE to false; '"' -> Keycodes.KEY_APOSTROPHE to true
            '`' -> Keycodes.KEY_GRAVE to false; '~' -> Keycodes.KEY_GRAVE to true
            ',' -> Keycodes.KEY_COMMA to false; '<' -> Keycodes.KEY_COMMA to true
            '.' -> Keycodes.KEY_DOT to false; '>' -> Keycodes.KEY_DOT to true
            '/' -> Keycodes.KEY_SLASH to false; '?' -> Keycodes.KEY_SLASH to true
            else -> null
        }
    }

    /** A keystroke plan: usage + modifier mask to hold. */
    data class Stroke(val usage: Int, val mods: Int)

    /** Splits text into strokes; unsupported characters become spaces (logged upstream). */
    fun strokesFor(text: String): List<Stroke> {
        val out = ArrayList<Stroke>(text.length)
        for (c in text) {
            val k = keystrokeFor(c) ?: run {
                out.add(Stroke(Keycodes.KEY_SPACE, 0)); continue
            }
            var mods = 0
            if (k.second) mods = mods or Keycodes.MOD_LSHIFT
            out.add(Stroke(k.first, mods))
        }
        return out
    }
}
