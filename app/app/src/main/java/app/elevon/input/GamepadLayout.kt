package app.elevon.input

import app.elevon.hid.Keycodes
import org.json.JSONArray
import org.json.JSONObject

/** What a gamepad control actually outputs to the computer. */
sealed class PadOutput {
    data class PadButton(val bit: Int) : PadOutput()
    data class PadStick(val stick: Int) : PadOutput() // 0 = left, 1 = right
    data class PadTrigger(val index: Int) : PadOutput() // 0 = LT, 1 = RT
    data class PadHat(val dir: Int) : PadOutput() // one of 4 dirs, combined at runtime
    data class Chords(val chords: List<Chord>) : PadOutput() // keyboard output
    data class MouseClick(val button: Int) : PadOutput()

    object MouseLook : PadOutput() // right-stick style camera via mouse deltas

    companion object {
        const val MOUSE_LEFT = 0x01
        const val MOUSE_RIGHT = 0x02
    }
}

enum class PadKind { BUTTON, STICK, DPAD, TRIGGER }

data class PadElement(
    val id: String,
    val kind: PadKind,
    val label: String,
    val x: Float, // 0..1, centre of the control
    val y: Float,
    val size: Float = 1f, // relative scale
    val output: PadOutput,
)

data class GamepadProfile(
    val id: String,
    val name: String,
    val elements: List<PadElement>,
    val deadZone: Float = 0.12f,
    val sensitivity: Float = 1f,
    val outputMode: OutputMode = OutputMode.GAMEPAD,
) {
    enum class OutputMode(val label: String, val note: String) {
        GAMEPAD("Gamepad", "Sends real gamepad inputs. Best for Steam and controller-aware games."),
        KEYBOARD_MOUSE("Keyboard & mouse", "Sends keys and mouse movement. Works with every game, including ones with no controller support."),
    }

    fun element(id: String): PadElement? = elements.firstOrNull { it.id == id }

    fun withElement(element: PadElement): GamepadProfile =
        copy(elements = elements.map { if (it.id == element.id) element else it })

    fun toJson(): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("name", name)
        root.put("deadZone", deadZone.toDouble())
        root.put("sensitivity", sensitivity.toDouble())
        root.put("outputMode", outputMode.name)
        val arr = JSONArray()
        for (e in elements) {
            val o = JSONObject()
            o.put("id", e.id)
            o.put("kind", e.kind.name)
            o.put("label", e.label)
            o.put("x", e.x.toDouble())
            o.put("y", e.y.toDouble())
            o.put("size", e.size.toDouble())
            o.put("output", PadOutputCodec.encode(e.output))
            arr.put(o)
        }
        root.put("elements", arr)
        return root.toString()
    }

    companion object {
        fun fromJson(text: String): GamepadProfile? = runCatching {
            val root = JSONObject(text)
            val arr = root.getJSONArray("elements")
            val elements = ArrayList<PadElement>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                elements.add(
                    PadElement(
                        id = o.getString("id"),
                        kind = PadKind.valueOf(o.getString("kind")),
                        label = o.getString("label"),
                        x = o.getDouble("x").toFloat(),
                        y = o.getDouble("y").toFloat(),
                        size = o.optDouble("size", 1.0).toFloat(),
                        output = PadOutputCodec.decode(o.getJSONObject("output")),
                    ),
                )
            }
            GamepadProfile(
                id = root.getString("id"),
                name = root.getString("name"),
                elements = elements,
                deadZone = root.optDouble("deadZone", 0.12).toFloat(),
                sensitivity = root.optDouble("sensitivity", 1.0).toFloat(),
                outputMode = runCatching {
                    OutputMode.valueOf(root.getString("outputMode"))
                }.getOrDefault(OutputMode.GAMEPAD),
            )
        }.getOrNull()
    }
}

/** JSON codec for [PadOutput] (org.json ships with Android). */
object PadOutputCodec {
    private const val TYPE_PAD_BUTTON = "padButton"
    private const val TYPE_PAD_STICK = "padStick"
    private const val TYPE_PAD_TRIGGER = "padTrigger"
    private const val TYPE_PAD_HAT = "padHat"
    private const val TYPE_CHORDS = "chords"
    private const val TYPE_MOUSE_CLICK = "mouseClick"
    private const val TYPE_MOUSE_LOOK = "mouseLook"

    fun encode(output: PadOutput): JSONObject {
        val o = JSONObject()
        when (output) {
            is PadOutput.PadButton -> {
                o.put("t", TYPE_PAD_BUTTON); o.put("bit", output.bit)
            }
            is PadOutput.PadStick -> {
                o.put("t", TYPE_PAD_STICK); o.put("stick", output.stick)
            }
            is PadOutput.PadTrigger -> {
                o.put("t", TYPE_PAD_TRIGGER); o.put("index", output.index)
            }
            is PadOutput.PadHat -> {
                o.put("t", TYPE_PAD_HAT); o.put("dir", output.dir)
            }
            is PadOutput.Chords -> {
                o.put("t", TYPE_CHORDS)
                val arr = JSONArray()
                for (c in output.chords) {
                    val c0 = JSONObject()
                    c0.put("mods", c.mods); c0.put("usage", c.usage)
                    arr.put(c0)
                }
                o.put("chords", arr)
            }
            is PadOutput.MouseClick -> {
                o.put("t", TYPE_MOUSE_CLICK); o.put("button", output.button)
            }
            PadOutput.MouseLook -> o.put("t", TYPE_MOUSE_LOOK)
        }
        return o
    }

    fun decode(o: JSONObject): PadOutput = when (o.optString("t")) {
        TYPE_PAD_BUTTON -> PadOutput.PadButton(o.getInt("bit"))
        TYPE_PAD_STICK -> PadOutput.PadStick(o.getInt("stick"))
        TYPE_PAD_TRIGGER -> PadOutput.PadTrigger(o.getInt("index"))
        TYPE_PAD_HAT -> PadOutput.PadHat(o.getInt("dir"))
        TYPE_CHORDS -> {
            val arr = o.getJSONArray("chords")
            val list = ArrayList<Chord>(arr.length())
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                list.add(Chord(c.getInt("mods"), c.getInt("usage")))
            }
            PadOutput.Chords(list)
        }
        TYPE_MOUSE_CLICK -> PadOutput.MouseClick(o.getInt("button"))
        else -> PadOutput.MouseLook
    }
}

/**
 * Built-in profiles. Presets named for common setups are honest about their
 * output mode: Minecraft and other games without controller support get the
 * keyboard & mouse mode, controller-aware games get the native gamepad.
 */
object GamepadProfiles {

    private fun btn(id: String, label: String, x: Float, y: Float, bit: Int, size: Float = 1f) =
        PadElement(id, PadKind.BUTTON, label, x, y, size, PadOutput.PadButton(bit))

    private fun trigger(id: String, label: String, x: Float, index: Int) =
        PadElement(id, PadKind.TRIGGER, label, x, 0.06f, 1f, PadOutput.PadTrigger(index))

    fun standard(id: String = "std", name: String = "Standard"): GamepadProfile = GamepadProfile(
        id = id,
        name = name,
        elements = listOf(
            PadElement(
                "dpad", PadKind.DPAD, "D-pad", 0.17f, 0.34f, 1f,
                PadOutput.PadHat(Keycodes.HAT_UP),
            ),
            PadElement(
                "stick_l", PadKind.STICK, "Left stick", 0.17f, 0.68f, 1f,
                PadOutput.PadStick(0),
            ),
            btn("a", "A", 0.87f, 0.40f, Keycodes.BTN_SOUTH),
            btn("b", "B", 0.95f, 0.30f, Keycodes.BTN_EAST),
            btn("x", "X", 0.79f, 0.30f, Keycodes.BTN_WEST),
            btn("y", "Y", 0.87f, 0.20f, Keycodes.BTN_NORTH),
            PadElement(
                "stick_r", PadKind.STICK, "Right stick", 0.83f, 0.68f, 1f,
                PadOutput.PadStick(1),
            ),
            btn("l1", "L1", 0.10f, 0.10f, Keycodes.BTN_L1, 1.4f),
            btn("r1", "R1", 0.90f, 0.10f, Keycodes.BTN_R1, 1.4f),
            trigger("l2", "L2", 0.28f, 0),
            trigger("r2", "R2", 0.72f, 1),
            btn("select", "—", 0.43f, 0.14f, Keycodes.BTN_SELECT, 0.8f),
            btn("start", "＋", 0.57f, 0.14f, Keycodes.BTN_START, 0.8f),
            btn("guide", "⌂", 0.50f, 0.55f, Keycodes.BTN_GUIDE, 0.8f),
        ),
    )

    fun minecraft(id: String = "mc", name: String = "Minecraft (keyboard)"): GamepadProfile =
        GamepadProfile(
            id = id,
            name = name,
            outputMode = GamepadProfile.OutputMode.KEYBOARD_MOUSE,
            elements = listOf(
                PadElement(
                    "dpad", PadKind.DPAD, "D-pad", 0.17f, 0.34f, 1f,
                    PadOutput.Chords(
                        listOf(
                            Chord(0, Keycodes.KEY_UP),
                            Chord(0, Keycodes.KEY_RIGHT),
                            Chord(0, Keycodes.KEY_DOWN),
                            Chord(0, Keycodes.KEY_LEFT),
                        ),
                    ),
                ),
                PadElement(
                    "stick_l", PadKind.STICK, "Move", 0.17f, 0.68f, 1f,
                    PadOutput.Chords(
                        listOf(
                            Chord(0, key('w')),
                            Chord(0, key('d')),
                            Chord(0, key('s')),
                            Chord(0, key('a')),
                        ),
                    ),
                ),
                btn("a", "Jump", 0.87f, 0.40f, -1, 1.2f)
                    .let { it.copy(output = PadOutput.Chords(listOf(Chord(0, Keycodes.KEY_SPACE)))) },
                btn("b", "Sneak", 0.95f, 0.30f, -1)
                    .let { it.copy(output = PadOutput.Chords(listOf(Chord(Keycodes.MOD_LSHIFT, 0)))) },
                btn("x", "Inventory", 0.79f, 0.30f, -1)
                    .let { it.copy(output = PadOutput.Chords(listOf(Chord(0, key('e'))))) },
                btn("y", "Drop", 0.87f, 0.20f, -1)
                    .let { it.copy(output = PadOutput.Chords(listOf(Chord(0, key('q'))))) },
                PadElement(
                    "stick_r", PadKind.STICK, "Look", 0.83f, 0.68f, 1f,
                    PadOutput.MouseLook,
                ),
                btn("l1", "Item 1", 0.10f, 0.10f, -1, 1.2f)
                    .let { it.copy(output = PadOutput.Chords(listOf(Chord(0, Keycodes.KEY_1)))) },
                btn("r1", "Use", 0.90f, 0.10f, -1, 1.2f)
                    .let { it.copy(output = PadOutput.MouseClick(PadOutput.MOUSE_RIGHT)) },
                trigger("l2", "Attack", 0.28f, 0)
                    .let { it.copy(output = PadOutput.MouseClick(PadOutput.MOUSE_LEFT)) },
                trigger("r2", "Item 2", 0.72f, 1)
                    .let { it.copy(output = PadOutput.Chords(listOf(Chord(0, Keycodes.KEY_2)))) },
                btn("select", "Esc", 0.43f, 0.14f, -1, 0.8f)
                    .let { it.copy(output = PadOutput.Chords(listOf(Chord(0, Keycodes.KEY_ESC)))) },
                btn("start", "Chat", 0.57f, 0.14f, -1, 0.8f)
                    .let { it.copy(output = PadOutput.Chords(listOf(Chord(0, key('t'))))) },
            ),
        )

    private fun key(c: Char): Int = Keycodes.KEY_A + (c.lowercaseChar() - 'a')

    fun defaults(): List<GamepadProfile> = listOf(
        standard(),
        minecraft(),
        standard("gta", "GTA V / controller games").let { it },
        standard("forza", "Forza / racing").copy(
            deadZone = 0.06f,
            sensitivity = 1.2f,
        ),
    )
}
