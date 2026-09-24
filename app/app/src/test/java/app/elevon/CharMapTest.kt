package app.elevon

import app.elevon.hid.CharMap
import app.elevon.hid.Keycodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CharMapTest {

    @Test
    fun `letters map without shift`() {
        val a = CharMap.keystrokeFor('a')!!
        assertEquals(Keycodes.KEY_A, a.first)
        assertFalse(a.second)
    }

    @Test
    fun `uppercase letters require shift`() {
        val z = CharMap.keystrokeFor('Z')!!
        assertEquals(Keycodes.KEY_Z, z.first)
        assertTrue(z.second)
    }

    @Test
    fun `digits and shifted symbols share usage`() {
        assertEquals(Keycodes.KEY_1 to false, CharMap.keystrokeFor('1'))
        assertEquals(Keycodes.KEY_1 to true, CharMap.keystrokeFor('!'))
        assertEquals(Keycodes.KEY_0 to true, CharMap.keystrokeFor(')'))
        assertEquals(Keycodes.KEY_SLASH to true, CharMap.keystrokeFor('?'))
    }

    @Test
    fun `enter tab space map`() {
        assertEquals(Keycodes.KEY_ENTER to false, CharMap.keystrokeFor('\n'))
        assertEquals(Keycodes.KEY_TAB to false, CharMap.keystrokeFor('\t'))
        assertEquals(Keycodes.KEY_SPACE to false, CharMap.keystrokeFor(' '))
    }

    @Test
    fun `unsupported characters become spaces in strokes`() {
        val strokes = CharMap.strokesFor("üñ")
        assertEquals(2, strokes.size)
        strokes.forEach { assertEquals(Keycodes.KEY_SPACE, it.usage) }
    }

    @Test
    fun `mixed case text keeps order`() {
        val strokes = CharMap.strokesFor("Ok")
        assertEquals(2, strokes.size)
        assertEquals(Keycodes.KEY_A + ('o' - 'a'), strokes[0].usage)
        assertTrue(strokes[0].mods and Keycodes.MOD_LSHIFT != 0)
        assertEquals(Keycodes.KEY_A + ('k' - 'a'), strokes[1].usage)
        assertEquals(0, strokes[1].mods)
    }

    @Test
    fun `unmapped control chars are not sent raw`() {
        assertNull(CharMap.keystrokeFor('\u20AC'))
        assertNull(CharMap.keystrokeFor('\u00FC'))
    }
}
