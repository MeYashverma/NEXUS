package app.elevon

import app.elevon.hid.GamepadReport
import app.elevon.hid.Keycodes
import app.elevon.hid.KeyboardReport
import app.elevon.hid.MouseReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardReportTest {

    @Test
    fun `empty report has null payload`() {
        val r = KeyboardReport()
        assertNull(r.payload())
    }

    @Test
    fun `press and release cycles slots`() {
        val r = KeyboardReport()
        assertTrue(r.press(Keycodes.KEY_A))
        val pressed = r.payload()!!
        assertEquals(7, pressed.size)
        assertEquals(Keycodes.KEY_A.toByte(), pressed[2])
        r.release(Keycodes.KEY_A)
        assertNull(r.payload())
    }

    @Test
    fun `six key rollover saturates at six`() {
        val r = KeyboardReport()
        for (k in 0..5) assertTrue(r.press(Keycodes.KEY_A + k))
        // 7th key is refused rather than corrupting the report
        assertFalse(r.press(Keycodes.KEY_A + 6))
        assertEquals(6, r.pressedKeys().size)
    }

    @Test
    fun `modifiers combine and clear`() {
        val r = KeyboardReport()
        r.addModifier(Keycodes.MOD_LCTRL)
        r.addModifier(Keycodes.MOD_LSHIFT)
        assertEquals(
            (Keycodes.MOD_LCTRL or Keycodes.MOD_LSHIFT).toByte(),
            r.payload()!![0],
        )
        r.clearModifiers()
        assertNull(r.payload())
    }

    @Test
    fun `pressing an already pressed key does not duplicate`() {
        val r = KeyboardReport()
        r.press(Keycodes.KEY_A)
        r.press(Keycodes.KEY_A)
        assertEquals(1, r.pressedKeys().size)
    }

    @Test
    fun `led output tracks caps lock`() {
        val r = KeyboardReport()
        assertFalse(r.capsLockOn())
        r.onOutputReport(byteArrayOf(0x02))
        assertTrue(r.capsLockOn())
        r.onOutputReport(byteArrayOf(0x00))
        assertFalse(r.capsLockOn())
    }
}

class MouseReportTest {

    @Test
    fun `idle report is null`() {
        assertNull(MouseReport().payload())
    }

    @Test
    fun `deltas accumulate and reset on send`() {
        val m = MouseReport()
        m.move(10, -5)
        m.move(3, 3)
        val p = m.payload()!!
        assertEquals(13, p[1].toInt())
        assertEquals(-2, p[2].toInt())
        assertNull(m.payload())
    }

    @Test
    fun `deltas clamp to int8 range`() {
        val m = MouseReport()
        m.move(500, -500)
        val p = m.payload()!!
        assertEquals(127, p[1].toInt())
        assertEquals(-127, p[2].toInt())
    }

    @Test
    fun `buttons set and clear`() {
        val m = MouseReport()
        m.setButton(MouseReport.BUTTON_LEFT, true)
        m.setButton(MouseReport.BUTTON_RIGHT, true)
        assertEquals(0x03, m.payload()!![0].toInt())
        m.setButton(MouseReport.BUTTON_LEFT, false)
        assertEquals(0x02, m.payload()!![0].toInt())
    }

    @Test
    fun `wheel accumulates`() {
        val m = MouseReport()
        m.scroll(3)
        assertEquals(3, m.payload()!![3].toInt())
    }
}

class GamepadReportTest {

    @Test
    fun `hat covers eight directions`() {
        assertEquals(Keycodes.HAT_UP, GamepadReport.hatFor(up = true, down = false, left = false, right = false))
        assertEquals(Keycodes.HAT_UP_RIGHT, GamepadReport.hatFor(true, false, false, true))
        assertEquals(Keycodes.HAT_RIGHT, GamepadReport.hatFor(false, false, false, true))
        assertEquals(Keycodes.HAT_DOWN_RIGHT, GamepadReport.hatFor(false, true, false, true))
        assertEquals(Keycodes.HAT_DOWN, GamepadReport.hatFor(false, true, false, false))
        assertEquals(Keycodes.HAT_DOWN_LEFT, GamepadReport.hatFor(false, true, true, false))
        assertEquals(Keycodes.HAT_LEFT, GamepadReport.hatFor(false, false, true, false))
        assertEquals(Keycodes.HAT_UP_LEFT, GamepadReport.hatFor(true, false, true, false))
        assertEquals(Keycodes.HAT_NEUTRAL, GamepadReport.hatFor(false, false, false, false))
    }

    @Test
    fun `dead zone snaps small inputs to zero`() {
        val (x, y) = GamepadReport.applyDeadZone(0.05f, 0.05f, 0.15f)
        assertEquals(0f, x)
        assertEquals(0f, y)
    }

    @Test
    fun `dead zone preserves direction beyond the zone`() {
        val (x, y) = GamepadReport.applyDeadZone(0f, 1f, 0.2f)
        assertEquals(0f, x)
        assertTrue(y > 0.7f)
    }

    @Test
    fun `payload packs nine bytes in descriptor order`() {
        val g = GamepadReport()
        g.setButton(Keycodes.BTN_SOUTH, true)
        g.setButton(Keycodes.BTN_START, true)
        g.setSticks(-127, 0, 127, 0)
        g.setTriggers(10, 250)
        g.setHat(Keycodes.HAT_UP_LEFT)
        val p = g.payload()
        assertEquals(9, p.size)
        assertEquals(0x01, p[0].toInt() and 0xFF) // South = bit 0
        assertEquals(0x02, p[1].toInt() and 0xFF) // Start = bit 9 → high byte bit 1
        assertEquals(-127, p[2].toInt())
        assertEquals(0, p[3].toInt())
        assertEquals(127, p[4].toInt())
        assertEquals(0, p[5].toInt())
        assertEquals(10, p[6].toInt() and 0xFF)
        assertEquals(250, p[7].toInt() and 0xFF)
        assertEquals(Keycodes.HAT_UP_LEFT.toByte(), p[8])
    }

    @Test
    fun `axis clamps work`() {
        assertEquals(127, GamepadReport.clampSigned(500))
        assertEquals(-127, GamepadReport.clampSigned(-500))
        assertEquals(255, GamepadReport.clampUnsigned(999))
        assertEquals(0, GamepadReport.clampUnsigned(-5))
        assertNotNull(GamepadReport().payload())
    }
}
