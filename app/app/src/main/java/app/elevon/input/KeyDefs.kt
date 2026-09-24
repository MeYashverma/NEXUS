package app.elevon.input

import app.elevon.hid.Keycodes

/**
 * Logical keyboard keys and the built-in layouts. Layouts are lists of rows;
 * every key has a stable id, a label and a HID meaning.
 */
enum class KeyKind { CHAR, MODIFIER, LOCK, FUNCTION }

data class KeyDef(
    val id: String,
    val label: String,
    val kind: KeyKind,
    val usage: Int = Keycodes.KEY_NONE,
    val mods: Int = 0,
    val shiftLabel: String? = null,
    val width: Float = 1f,
) {
    companion object {
        fun char(label: String, usage: Int, shiftLabel: String? = null, width: Float = 1f) =
            KeyDef(label, label, KeyKind.CHAR, usage, 0, shiftLabel, width)

        /** Explicit id + label form used by layouts that show different text. */
        fun char(id: String, label: String, usage: Int, shiftLabel: String? = null, width: Float = 1f) =
            KeyDef(id, label, KeyKind.CHAR, usage, 0, shiftLabel, width)

        fun mod(id: String, label: String, mods: Int, width: Float = 1f) =
            KeyDef(id, label, KeyKind.MODIFIER, Keycodes.KEY_NONE, mods, null, width)

        fun fn(id: String, label: String, usage: Int, width: Float = 1f) =
            KeyDef(id, label, KeyKind.FUNCTION, usage, 0, null, width)

        val SHIFT = mod("shift", "Shift", Keycodes.MOD_LSHIFT, 1.5f)
        val CTRL = mod("ctrl", "Ctrl", Keycodes.MOD_LCTRL)
        val ALT = mod("alt", "Alt", Keycodes.MOD_LALT)
        val GUI = mod("gui", "Win", Keycodes.MOD_LGUI)
        val CAPS = KeyDef("caps", "Caps", KeyKind.LOCK, Keycodes.KEY_CAPSLOCK)
    }
}

enum class KeyboardLayoutId(val label: String, val description: String) {
    COMPACT("Compact", "One-handed quick typing"),
    FULL("Full", "Traditional desktop rows"),
    GAMING("Gaming", "WASD cluster and game keys"),
    CUSTOM("Custom", "Your own arrangement"),
}

object KeyLayouts {

    private fun f(n: Int) = KeyDef.fn("f$n", "F$n", Keycodes.KEY_F1 + n - 1, 1f)

    /** Function strip, shown as an optional top row. */
    val functionRow: List<KeyDef> = buildList {
        add(KeyDef.fn("esc", "Esc", Keycodes.KEY_ESC, 1.4f))
        for (i in 1..12) add(f(i))
        add(KeyDef.fn("del", "Del", Keycodes.KEY_DELETE, 1.4f))
    }

    val compact: List<List<KeyDef>> = listOf(
        listOf(
            KeyDef.char("q", Keycodes.KEY_A + ('q' - 'a')),
            KeyDef.char("w", Keycodes.KEY_A + ('w' - 'a')),
            KeyDef.char("e", Keycodes.KEY_A + ('e' - 'a')),
            KeyDef.char("r", Keycodes.KEY_A + ('r' - 'a')),
            KeyDef.char("t", Keycodes.KEY_A + ('t' - 'a')),
            KeyDef.char("y", Keycodes.KEY_A + ('y' - 'a')),
            KeyDef.char("u", Keycodes.KEY_A + ('u' - 'a')),
            KeyDef.char("i", Keycodes.KEY_A + ('i' - 'a')),
            KeyDef.char("o", Keycodes.KEY_A + ('o' - 'a')),
            KeyDef.char("p", Keycodes.KEY_A + ('p' - 'a')),
            KeyDef.fn("bksp", "⌫", Keycodes.KEY_BACKSPACE, 1.4f),
        ),
        listOf(
            KeyDef.char("a", Keycodes.KEY_A + ('a' - 'a')),
            KeyDef.char("s", Keycodes.KEY_A + ('s' - 'a')),
            KeyDef.char("d", Keycodes.KEY_A + ('d' - 'a')),
            KeyDef.char("f", Keycodes.KEY_A + ('f' - 'a')),
            KeyDef.char("g", Keycodes.KEY_A + ('g' - 'a')),
            KeyDef.char("h", Keycodes.KEY_A + ('h' - 'a')),
            KeyDef.char("j", Keycodes.KEY_A + ('j' - 'a')),
            KeyDef.char("k", Keycodes.KEY_A + ('k' - 'a')),
            KeyDef.char("l", Keycodes.KEY_A + ('l' - 'a')),
            KeyDef.char("semicolon", ";", Keycodes.KEY_SEMICOLON, shiftLabel = ":"),
            KeyDef.char("apostrophe", "'", Keycodes.KEY_APOSTROPHE, shiftLabel = "\""),
        ),
        listOf(
            KeyDef.SHIFT.copy(width = 1.4f),
            KeyDef.char("z", Keycodes.KEY_A + ('z' - 'a')),
            KeyDef.char("x", Keycodes.KEY_A + ('x' - 'a')),
            KeyDef.char("c", Keycodes.KEY_A + ('c' - 'a')),
            KeyDef.char("v", Keycodes.KEY_A + ('v' - 'a')),
            KeyDef.char("b", Keycodes.KEY_A + ('b' - 'a')),
            KeyDef.char("n", Keycodes.KEY_A + ('n' - 'a')),
            KeyDef.char("m", Keycodes.KEY_A + ('m' - 'a')),
            KeyDef.char("comma", ",", Keycodes.KEY_COMMA, shiftLabel = "<"),
            KeyDef.char("dot", ".", Keycodes.KEY_DOT, shiftLabel = ">"),
            KeyDef.char("slash", "/", Keycodes.KEY_SLASH, shiftLabel = "?"),
        ),
        listOf(
            KeyDef.CTRL,
            KeyDef.ALT,
            KeyDef.char("space", "space", Keycodes.KEY_SPACE, width = 5f),
            KeyDef.fn("enter", "enter", Keycodes.KEY_ENTER, 1.6f),
            KeyDef.fn("tab", "tab", Keycodes.KEY_TAB, 1.4f),
        ),
    )

    val full: List<List<KeyDef>> = listOf(
        listOf(KeyDef.fn("esc", "Esc", Keycodes.KEY_ESC)) +
            (1..12).map { f(it) } +
            listOf(
                KeyDef.fn("ins", "Ins", Keycodes.KEY_INSERT),
                KeyDef.fn("del", "Del", Keycodes.KEY_DELETE),
            ),
        listOf(
            KeyDef.char("grave", "`", Keycodes.KEY_GRAVE, shiftLabel = "~"),
            KeyDef.char("1", "1", Keycodes.KEY_1, shiftLabel = "!"),
            KeyDef.char("2", "2", Keycodes.KEY_2, shiftLabel = "@"),
            KeyDef.char("3", "3", Keycodes.KEY_3, shiftLabel = "#"),
            KeyDef.char("4", "4", Keycodes.KEY_4, shiftLabel = "$"),
            KeyDef.char("5", "5", Keycodes.KEY_5, shiftLabel = "%"),
            KeyDef.char("6", "6", Keycodes.KEY_6, shiftLabel = "^"),
            KeyDef.char("7", "7", Keycodes.KEY_7, shiftLabel = "&"),
            KeyDef.char("8", "8", Keycodes.KEY_8, shiftLabel = "*"),
            KeyDef.char("9", "9", Keycodes.KEY_9, shiftLabel = "("),
            KeyDef.char("0", "0", Keycodes.KEY_0, shiftLabel = ")"),
            KeyDef.char("minus", "-", Keycodes.KEY_MINUS, shiftLabel = "_"),
            KeyDef.char("equal", "=", Keycodes.KEY_EQUAL, shiftLabel = "+"),
            KeyDef.fn("bksp", "⌫", Keycodes.KEY_BACKSPACE, width = 2f),
        ),
        listOf(
            KeyDef.fn("tab", "tab", Keycodes.KEY_TAB, width = 1.5f),
            KeyDef.char("q", Keycodes.KEY_A + ('q' - 'a')),
            KeyDef.char("w", Keycodes.KEY_A + ('w' - 'a')),
            KeyDef.char("e", Keycodes.KEY_A + ('e' - 'a')),
            KeyDef.char("r", Keycodes.KEY_A + ('r' - 'a')),
            KeyDef.char("t", Keycodes.KEY_A + ('t' - 'a')),
            KeyDef.char("y", Keycodes.KEY_A + ('y' - 'a')),
            KeyDef.char("u", Keycodes.KEY_A + ('u' - 'a')),
            KeyDef.char("i", Keycodes.KEY_A + ('i' - 'a')),
            KeyDef.char("o", Keycodes.KEY_A + ('o' - 'a')),
            KeyDef.char("p", Keycodes.KEY_A + ('p' - 'a')),
            KeyDef.char("lbracket", "[", Keycodes.KEY_LEFTBRACKET, shiftLabel = "{"),
            KeyDef.char("rbracket", "]", Keycodes.KEY_RIGHTBRACKET, shiftLabel = "}"),
            KeyDef.char("backslash", "\\", Keycodes.KEY_BACKSLASH, shiftLabel = "|", width = 1.5f),
        ),
        listOf(
            KeyDef.CAPS.copy(width = 1.75f),
            KeyDef.char("a", Keycodes.KEY_A + ('a' - 'a')),
            KeyDef.char("s", Keycodes.KEY_A + ('s' - 'a')),
            KeyDef.char("d", Keycodes.KEY_A + ('d' - 'a')),
            KeyDef.char("f", Keycodes.KEY_A + ('f' - 'a')),
            KeyDef.char("g", Keycodes.KEY_A + ('g' - 'a')),
            KeyDef.char("h", Keycodes.KEY_A + ('h' - 'a')),
            KeyDef.char("j", Keycodes.KEY_A + ('j' - 'a')),
            KeyDef.char("k", Keycodes.KEY_A + ('k' - 'a')),
            KeyDef.char("l", Keycodes.KEY_A + ('l' - 'a')),
            KeyDef.char("semicolon", ";", Keycodes.KEY_SEMICOLON, shiftLabel = ":"),
            KeyDef.char("apostrophe", "'", Keycodes.KEY_APOSTROPHE, shiftLabel = "\""),
            KeyDef.fn("enter", "enter", Keycodes.KEY_ENTER, width = 2.25f),
        ),
        listOf(
            KeyDef.SHIFT.copy(width = 2.25f),
            KeyDef.char("z", Keycodes.KEY_A + ('z' - 'a')),
            KeyDef.char("x", Keycodes.KEY_A + ('x' - 'a')),
            KeyDef.char("c", Keycodes.KEY_A + ('c' - 'a')),
            KeyDef.char("v", Keycodes.KEY_A + ('v' - 'a')),
            KeyDef.char("b", Keycodes.KEY_A + ('b' - 'a')),
            KeyDef.char("n", Keycodes.KEY_A + ('n' - 'a')),
            KeyDef.char("m", Keycodes.KEY_A + ('m' - 'a')),
            KeyDef.char("comma", ",", Keycodes.KEY_COMMA, shiftLabel = "<"),
            KeyDef.char("dot", ".", Keycodes.KEY_DOT, shiftLabel = ">"),
            KeyDef.char("slash", "/", Keycodes.KEY_SLASH, shiftLabel = "?"),
            KeyDef.SHIFT.copy(id = "shift_r", width = 2.75f),
        ),
        listOf(
            KeyDef.CTRL.copy(width = 1.25f),
            KeyDef.GUI.copy(width = 1.25f),
            KeyDef.ALT.copy(width = 1.25f),
            KeyDef.char("space", "space", Keycodes.KEY_SPACE, width = 6.25f),
            KeyDef.ALT.copy(id = "alt_r", width = 1.25f),
            KeyDef.CTRL.copy(id = "ctrl_r", mods = Keycodes.MOD_RCTRL, width = 1.25f),
        ) + navCluster(),
    )

    private fun navCluster(): List<KeyDef> = listOf(
        KeyDef.fn("left", "←", Keycodes.KEY_LEFT),
        KeyDef.fn("up", "↑", Keycodes.KEY_UP),
        KeyDef.fn("down", "↓", Keycodes.KEY_DOWN),
        KeyDef.fn("right", "→", Keycodes.KEY_RIGHT),
    ).let { arrows ->
        // Represented as a mini column on screen; flat list with ids preserved.
        listOf(
            KeyDef.fn("home", "Home", Keycodes.KEY_HOME, width = 0f),
        ) + arrows
    }.filter { it.width > 0f }

    val gaming: List<List<KeyDef>> = listOf(
        listOf(
            KeyDef.fn("esc", "Esc", Keycodes.KEY_ESC, width = 1.3f),
            KeyDef.char("1", "1", Keycodes.KEY_1),
            KeyDef.char("2", "2", Keycodes.KEY_2),
            KeyDef.char("3", "3", Keycodes.KEY_3),
            KeyDef.char("4", "4", Keycodes.KEY_4),
            KeyDef.char("5", "5", Keycodes.KEY_5),
            KeyDef.char("6", "6", Keycodes.KEY_6),
            KeyDef.char("r", Keycodes.KEY_A + ('r' - 'a')),
            KeyDef.char("f", Keycodes.KEY_A + ('f' - 'a')),
            KeyDef.char("c", Keycodes.KEY_A + ('c' - 'a')),
            KeyDef.fn("tab", "tab", Keycodes.KEY_TAB, width = 1.3f),
        ),
        listOf(
            KeyDef.char("q", Keycodes.KEY_A + ('q' - 'a')),
            KeyDef.char("w", Keycodes.KEY_A + ('w' - 'a')),
            KeyDef.char("e", Keycodes.KEY_A + ('e' - 'a')),
            KeyDef.char("g", Keycodes.KEY_A + ('g' - 'a')),
            KeyDef.char("space", "space", Keycodes.KEY_SPACE, width = 3f),
            KeyDef.SHIFT.copy(width = 2f),
            KeyDef.char("z", Keycodes.KEY_A + ('z' - 'a')),
            KeyDef.char("x", Keycodes.KEY_A + ('x' - 'a')),
            KeyDef.char("v", Keycodes.KEY_A + ('v' - 'a')),
        ),
        listOf(
            KeyDef.CTRL.copy(width = 1.6f),
            KeyDef.char("a", Keycodes.KEY_A + ('a' - 'a')),
            KeyDef.char("s", Keycodes.KEY_A + ('s' - 'a')),
            KeyDef.char("d", Keycodes.KEY_A + ('d' - 'a')),
            KeyDef.char("b", Keycodes.KEY_A + ('b' - 'a')),
            KeyDef.ALT.copy(width = 1.6f),
            KeyDef.fn("up", "↑", Keycodes.KEY_UP),
            KeyDef.fn("down", "↓", Keycodes.KEY_DOWN),
            KeyDef.fn("left", "←", Keycodes.KEY_LEFT),
            KeyDef.fn("right", "→", Keycodes.KEY_RIGHT),
        ),
    )
}
