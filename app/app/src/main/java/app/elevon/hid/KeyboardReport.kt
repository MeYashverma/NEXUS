package app.elevon.hid

/**
 * Builds keyboard input report payloads (report ID 1, payload without the ID):
 * `[mods, reserved, k1..k6]` with 6-key rollover.
 *
 * Pure JVM logic — covered by unit tests.
 */
class KeyboardReport {

    private val slots = IntArray(6) { Keycodes.KEY_NONE }
    private var modifiers = 0

    fun modifierState(): Int = modifiers

    /** Pressed-key usages in slot order (for UI echo). */
    fun pressedKeys(): List<Int> = slots.filter { it != Keycodes.KEY_NONE }

    fun addModifier(mask: Int) { modifiers = modifiers or mask }

    fun removeModifier(mask: Int) { modifiers = modifiers and mask.inv() }

    fun clearModifiers() { modifiers = 0 }

    /**
     * Presses [usage]. Returns false when all six slots are busy
     * (ghosting guard: the report simply doesn't grow).
     */
    fun press(usage: Int): Boolean {
        if (usage in Keycodes.KEY_NONE..Keycodes.KEY_ERROR_ROLLOVER) return false
        if (slots.contains(usage)) return true
        val free = slots.indexOfFirst { it == Keycodes.KEY_NONE }
        if (free < 0) return false
        slots[free] = usage
        return true
    }

    fun release(usage: Int) {
        for (i in slots.indices) if (slots[i] == usage) slots[i] = Keycodes.KEY_NONE
    }

    fun releaseAll() {
        for (i in slots.indices) slots[i] = Keycodes.KEY_NONE
        modifiers = 0
    }

    /** Serialises the current state; returns null when nothing is pressed. */
    fun payload(): ByteArray? {
        if (modifiers == 0 && slots.all { it == Keycodes.KEY_NONE }) return null
        val out = ByteArray(HidDescriptors.keyboardPayloadSize)
        out[0] = modifiers.toByte()
        out[1] = 0
        var s = 0
        for (k in slots) out[2 + s++] = k.toByte()
        return out
    }

    fun capsLockOn(): Boolean = ledCapsLock

    /** Track the Caps Lock LED the host reports back so the UI can show lock state. */
    fun onOutputReport(data: ByteArray) {
        if (data.isNotEmpty()) {
            ledCapsLock = (data[0].toInt() and 0x02) != 0
            ledNumLock = (data[0].toInt() and 0x01) != 0
            ledScrollLock = (data[0].toInt() and 0x04) != 0
        }
    }

    var ledCapsLock: Boolean = false
        private set
    var ledNumLock: Boolean = false
        private set
    var ledScrollLock: Boolean = false
        private set
}
