package app.elevon

import app.elevon.data.Profile
import app.elevon.input.Chord
import app.elevon.input.describe
import app.elevon.input.GamepadProfile
import app.elevon.input.GamepadProfiles
import app.elevon.input.MacroAction
import app.elevon.input.MacroPage
import app.elevon.input.MacroPresets
import app.elevon.input.MacroStep
import app.elevon.input.PadOutput
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelJsonTest {

    @Test
    fun `gamepad profile json round trip`() {
        val profile = GamepadProfiles.minecraft()
        val restored = GamepadProfile.fromJson(profile.toJson())!!
        assertEquals(profile.id, restored.id)
        assertEquals(profile.name, restored.name)
        assertEquals(profile.elements.size, restored.elements.size)
        assertEquals(profile.outputMode, restored.outputMode)
        val originalStick = profile.elements.first { it.kind == app.elevon.input.PadKind.STICK }
        val restoredStick = restored.elements.first { it.kind == app.elevon.input.PadKind.STICK }
        assertEquals(originalStick.output, restoredStick.output)
    }

    @Test
    fun `corrupt json decodes to null not crash`() {
        assertNull(GamepadProfile.fromJson("{not json"))
        assertNull(MacroPage.fromJson("]]]"))
    }

    @Test
    fun `macro page json round trip preserves empty slots`() {
        val page = MacroPage(
            "p1",
            "Test",
            listOf(
                MacroAction("a", "Copy", "Ctrl+C", listOf(MacroStep.Chord(1, 6))),
                null,
                MacroAction("b", "Type", "Types text", listOf(MacroStep.Text("hi"))),
            ) + List(9) { null },
        )
        val restored = MacroPage.fromJson(page.toJson())!!
        assertEquals(12, restored.buttons.size)
        assertEquals("Copy", restored.buttons[0]?.label)
        assertNull(restored.buttons[1])
        assertEquals("hi", (restored.buttons[2]?.steps?.first() as MacroStep.Text).text)
    }

    @Test
    fun `presets have twelve slots`() {
        val productivity = MacroPresets.productivity(app.elevon.input.HostOs.MACOS)
        val media = MacroPresets.media()
        assertEquals(12, productivity.size)
        assertEquals(12, media.size)
        assertTrue(productivity[0]!!.steps.first() is MacroStep.Chord)
    }

    @Test
    fun `mac shortcuts use command modifier`() {
        val s = MacroPresets.productivity(app.elevon.input.HostOs.MACOS)
        val copy = s.first { it?.label == "Copy" }!!
        val chord = copy.steps.first() as MacroStep.Chord
        assertEquals(app.elevon.hid.Keycodes.MOD_LGUI, chord.mods)
    }

    @Test
    fun `profile defaults parse`() {
        Profile.defaults().forEach { p ->
            val restored = Profile.fromJson(p.toJson())
            assertEquals(p, restored)
        }
    }

    @Test
    fun `chord describe renders readable combos`() {
        assertEquals(
            "Ctrl+Shift+key",
            Chord(
                app.elevon.hid.Keycodes.MOD_LCTRL or app.elevon.hid.Keycodes.MOD_LSHIFT,
                app.elevon.hid.Keycodes.KEY_ESC,
            ).describe(),
        )
        assertEquals("Cmd+key", Chord(app.elevon.hid.Keycodes.MOD_LGUI, app.elevon.hid.Keycodes.KEY_A + 2).describe())
    }

    @Test
    fun `pad output codec round trips all kinds`() {
        val outputs = listOf(
            PadOutput.PadButton(3),
            PadOutput.PadStick(1),
            PadOutput.PadTrigger(0),
            PadOutput.PadHat(0),
            PadOutput.Chords(listOf(Chord(2, 11), Chord(0, 26))),
            PadOutput.MouseClick(PadOutput.MOUSE_LEFT),
            PadOutput.MouseLook,
        )
        outputs.forEach { original ->
            val decoded = app.elevon.input.PadOutputCodec.decode(app.elevon.input.PadOutputCodec.encode(original))
            assertEquals(original, decoded)
        }
        assertNotNull(JSONArray().length())
    }
}
