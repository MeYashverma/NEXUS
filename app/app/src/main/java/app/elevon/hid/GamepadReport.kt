package app.elevon.hid

/**
 * Gamepad input report payload (report ID 5, payload without the ID):
 * `[btnLo, btnHi, X, Y, Z, Rz, Rx, Ry, hat]`.
 *
 * Axes: -127..127 (0 centred). Triggers: 0..255. Hat: 0..7 or 15 (neutral).
 * Button order matches the generic-HID conventions most launchers map
 * (South, East, West, North, L1, R1, L3, R3, Select, Start, Guide) — see
 * docs/compatibility.md for the honest story about XInput-only games.
 */
class GamepadReport {

    data class State(
        val buttons: Int = 0,
        val lx: Int = 0, val ly: Int = 0,
        val rx: Int = 0, val ry: Int = 0,
        val lt: Int = 0, val rt: Int = 0,
        val hat: Int = Keycodes.HAT_NEUTRAL,
    )

    private var buttons = 0
    private var lx = 0; private var ly = 0
    private var rx = 0; private var ry = 0
    private var lt = 0; private var rt = 0
    private var hat = Keycodes.HAT_NEUTRAL

    fun setButton(bit: Int, down: Boolean) {
        buttons = if (down) buttons or (1 shl bit) else buttons and (1 shl bit).inv()
    }

    fun setSticks(lx: Int, ly: Int, rx: Int, ry: Int) {
        this.lx = clampSigned(lx); this.ly = clampSigned(ly)
        this.rx = clampSigned(rx); this.ry = clampSigned(ry)
    }

    fun setTriggers(lt: Int, rt: Int) {
        this.lt = clampUnsigned(lt); this.rt = clampUnsigned(rt)
    }

    fun setHat(hat: Int) { this.hat = hat }

    fun current(): State = State(buttons, lx, ly, rx, ry, lt, rt, hat)

    fun reset() {
        buttons = 0; lx = 0; ly = 0; rx = 0; ry = 0; lt = 0; rt = 0
        hat = Keycodes.HAT_NEUTRAL
    }

    /** Serialises the current state; always sends while the gamepad mode is live. */
    fun payload(): ByteArray {
        val out = ByteArray(HidDescriptors.gamepadPayloadSize)
        out[0] = (buttons and 0xFF).toByte()
        out[1] = ((buttons shr 8) and 0xFF).toByte()
        out[2] = lx.toByte()
        out[3] = ly.toByte()
        out[4] = rx.toByte()
        out[5] = ry.toByte()
        out[6] = lt.toByte()
        out[7] = rt.toByte()
        out[8] = hat.toByte()
        return out
    }

    companion object {
        const val AXIS_MIN = -127
        const val AXIS_MAX = 127
        const val TRIGGER_MAX = 255

        fun clampSigned(v: Int) = v.coerceIn(AXIS_MIN, AXIS_MAX)
        fun clampUnsigned(v: Int) = v.coerceIn(0, TRIGGER_MAX)

        /** Hat value for an 8-direction D-pad; null state when no direction. */
        fun hatFor(up: Boolean, down: Boolean, left: Boolean, right: Boolean): Int = when {
            up && right -> Keycodes.HAT_UP_RIGHT
            down && right -> Keycodes.HAT_DOWN_RIGHT
            down && left -> Keycodes.HAT_DOWN_LEFT
            up && left -> Keycodes.HAT_UP_LEFT
            up -> Keycodes.HAT_UP
            down -> Keycodes.HAT_DOWN
            left -> Keycodes.HAT_LEFT
            right -> Keycodes.HAT_RIGHT
            else -> Keycodes.HAT_NEUTRAL
        }

        /** Applies a radial dead zone: outputs below the zone snap to 0. */
        fun applyDeadZone(x: Float, y: Float, deadZone: Float): Pair<Float, Float> {
            val mag = kotlin.math.sqrt(x * x + y * y)
            if (mag <= deadZone) return 0f to 0f
            if (mag <= 0.0001f) return x to y
            val scaled = ((mag - deadZone) / (1f - deadZone)).coerceIn(0f, 1f)
            val factor = scaled / mag
            return (x * factor) to (y * factor)
        }
    }
}
