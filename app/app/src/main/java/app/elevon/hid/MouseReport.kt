package app.elevon.hid

/**
 * Mouse input report payload (report ID 2, payload without the ID):
 * `[buttons, x, y, wheel, acPan]`, deltas clamped to ±127.
 */
class MouseReport {
    private var buttons = 0
    private var accX = 0
    private var accY = 0
    private var wheel = 0
    private var pan = 0

    companion object {
        const val BUTTON_LEFT = 0x01
        const val BUTTON_RIGHT = 0x02
        const val BUTTON_MIDDLE = 0x04
        const val MAX_DELTA = 127
        const val MIN_DELTA = -127
    }

    fun setButton(mask: Int, down: Boolean) {
        buttons = if (down) buttons or mask else buttons and mask.inv()
    }

    fun isButtonDown(mask: Int): Boolean = (buttons and mask) != 0

    fun move(dx: Int, dy: Int) {
        accX = clamp(accX + dx)
        accY = clamp(accY + dy)
    }

    fun scroll(notches: Int) { wheel = clamp(wheel + notches) }

    fun panHorizontal(notches: Int) { pan = clamp(pan + notches) }

    /** Serialises and resets accumulated deltas. Always returns a report so button releases are sent. */
    fun payload(): ByteArray {
        val out = ByteArray(HidDescriptors.mousePayloadSize)
        out[0] = buttons.toByte()
        out[1] = accX.toByte()
        out[2] = accY.toByte()
        out[3] = wheel.toByte()
        out[4] = pan.toByte()
        accX = 0; accY = 0; wheel = 0; pan = 0
        return out
    }

    /** Returns null when truly idle (no buttons, no deltas) — used to avoid spamming empty reports in loops. */
    fun payloadOrNullIfIdle(): ByteArray? {
        if (buttons == 0 && accX == 0 && accY == 0 && wheel == 0 && pan == 0) return null
        return payload()
    }

    private fun clamp(v: Int) = v.coerceIn(MIN_DELTA, MAX_DELTA)
}
