package app.elevon.hid

/**
 * HID report descriptor and report IDs for the single composite Elevon device.
 *
 * Why one descriptor: hosts cache the report descriptor at pairing time
 * (see docs/research.md, "Descriptor caching"). Elevon therefore presents
 * ONE composite device — keyboard + mouse + consumer + system + gamepad —
 * so a single pairing unlocks every control mode. A second, keyboard+mouse
 * only descriptor is offered as a "compatibility" profile for fussy hosts;
 * switching descriptors requires re-pairing and the UI says so.
 *
 * Report payloads passed to [android.bluetooth.BluetoothHidDevice.sendReport]
 * do NOT include the report ID (the stack prepends it):
 *
 *  ID 1 keyboard : mods(1) reserved(1) keys(6)              -> 7 bytes
 *  ID 2 mouse    : buttons(1) X(1) Y(1) wheel(1) acPan(1)   -> 5 bytes
 *  ID 3 consumer : usage low(1) usage high(1)               -> 2 bytes
 *  ID 4 system   : code(1)                                  -> 1 byte
 *  ID 5 gamepad  : buttons(2) X Y Z Rz(4) Rx Ry(2) hat(1)   -> 9 bytes
 */
object HidDescriptors {

    const val REPORT_ID_KEYBOARD = 1
    const val REPORT_ID_MOUSE = 2
    const val REPORT_ID_CONSUMER = 3
    const val REPORT_ID_SYSTEM = 4
    const val REPORT_ID_GAMEPAD = 5

    val keyboardPayloadSize = 7
    val mousePayloadSize = 5
    val consumerPayloadSize = 2
    val systemPayloadSize = 1
    val gamepadPayloadSize = 9

    /** Keyboard + mouse only (compatibility descriptor, no consumer/system/gamepad). */
    fun basic(): ByteArray = buildList {
        keyboard()
        mouse()
    }.toByteArray()

    /** The full composite descriptor (default). */
    fun composite(): ByteArray = buildList {
        keyboard()
        mouse()
        consumer()
        system()
        gamepad()
    }.toByteArray()

    private fun MutableList<Int>.keyboard() {
        // Usage Page (Generic Desktop)
        add(0x05); add(0x01)
        // Usage (Keyboard)
        add(0x09); add(0x06)
        // Collection (Application)
        add(0xA1); add(0x01)
        //   Report ID (1)
        add(0x85); add(REPORT_ID_KEYBOARD)
        //   Usage Page (Keyboard/Keypad)
        add(0x05); add(0x07)
        //   Usage Minimum (Left Control) ... Usage Maximum (Right GUI)
        add(0x19); add(0xE0)
        add(0x29); add(0xE7)
        //   Logical 0..1, 1 bit x 8 -> modifier byte
        add(0x15); add(0x00)
        add(0x25); add(0x01)
        add(0x75); add(0x01)
        add(0x95); add(0x08)
        add(0x81); add(0x02) // Input (Data, Var, Abs)
        //   Reserved byte
        add(0x95); add(0x01)
        add(0x75); add(0x08)
        add(0x81); add(0x01) // Input (Const, Arr, Abs)
        //   LED output report (Num Lock, Caps Lock, Scroll Lock, Compose, Kana)
        add(0x95); add(0x05)
        add(0x75); add(0x01)
        add(0x05); add(0x08) // Usage Page (LEDs)
        add(0x19); add(0x01)
        add(0x29); add(0x05)
        add(0x91); add(0x02) // Output (Data, Var, Abs)
        add(0x95); add(0x01)
        add(0x75); add(0x03)
        add(0x91); add(0x01) // Output (Const, Var, Abs)
        //   6-key rollover key array
        add(0x95); add(0x06)
        add(0x75); add(0x08)
        add(0x15); add(0x00)
        add(0x25); add(0x65)
        add(0x05); add(0x07) // Usage Page (Keyboard/Keypad)
        add(0x19); add(0x00)
        add(0x29); add(0x65)
        add(0x81); add(0x00) // Input (Data, Arr, Abs)
        add(0xC0) // End Collection
    }

    private fun MutableList<Int>.mouse() {
        add(0x05); add(0x01) // Usage Page (Generic Desktop)
        add(0x09); add(0x02) // Usage (Mouse)
        add(0xA1); add(0x01) // Collection (Application)
        add(0x85); add(REPORT_ID_MOUSE)
        add(0x09); add(0x01) // Usage (Pointer)
        add(0xA1); add(0x00) // Collection (Physical)
        //   Buttons 1..3
        add(0x05); add(0x09) // Usage Page (Button)
        add(0x19); add(0x01)
        add(0x29); add(0x03)
        add(0x15); add(0x00)
        add(0x25); add(0x01)
        add(0x75); add(0x01)
        add(0x95); add(0x03)
        add(0x81); add(0x02) // Input (Data, Var, Abs)
        //   5 bits padding
        add(0x95); add(0x01)
        add(0x75); add(0x05)
        add(0x81); add(0x03) // Input (Const, Var, Abs)
        //   X, Y relative + vertical wheel, all -127..127
        add(0x05); add(0x01) // Usage Page (Generic Desktop)
        add(0x09); add(0x30) // X
        add(0x09); add(0x31) // Y
        add(0x09); add(0x38) // Wheel
        add(0x15); add(0x81) // Logical Minimum (-127)
        add(0x25); add(0x7F) // Logical Maximum (127)
        add(0x75); add(0x08)
        add(0x95); add(0x03)
        add(0x81); add(0x06) // Input (Data, Var, Rel)
        //   AC Pan (horizontal scroll wheel)
        add(0x05); add(0x0C) // Usage Page (Consumer)
        add(0x0A); add(0x38); add(0x02) // AC Pan
        add(0x95); add(0x01)
        add(0x81); add(0x06) // Input (Data, Var, Rel)
        add(0xC0) // End Collection (Physical)
        add(0xC0) // End Collection (Application)
    }

    private fun MutableList<Int>.consumer() {
        add(0x05); add(0x0C) // Usage Page (Consumer)
        add(0x09); add(0x01) // Usage (Consumer Control)
        add(0xA1); add(0x01) // Collection (Application)
        add(0x85); add(REPORT_ID_CONSUMER)
        add(0x15); add(0x00) // Logical Minimum (0)
        add(0x26); add(0xFF); add(0x0F) // Logical Maximum (4095)
        add(0x19); add(0x00) // Usage Minimum (0)
        add(0x2A); add(0xFF); add(0x0F) // Usage Maximum (4095)
        add(0x75); add(0x10) // Report Size (16)
        add(0x95); add(0x01) // Report Count (1)
        add(0x81); add(0x00) // Input (Data, Arr, Abs)
        add(0x75); add(0x10) // Report Size (16)
        add(0x95); add(0x01) // Report Count (1)
        add(0x81); add(0x03) // Input (Const, Var, Abs) — padding
        add(0xC0) // End Collection
    }

    private fun MutableList<Int>.system() {
        add(0x05); add(0x01) // Usage Page (Generic Desktop)
        add(0x09); add(0x80) // Usage (System Control)
        add(0xA1); add(0x01) // Collection (Application)
        add(0x85); add(REPORT_ID_SYSTEM)
        //   2-bit array: 1 = Sleep, 2 = Power Down, 3 = Wake Up
        add(0x75); add(0x02)
        add(0x95); add(0x01)
        add(0x15); add(0x01)
        add(0x25); add(0x03)
        add(0x09); add(0x82) // System Sleep
        add(0x09); add(0x81) // System Power Down
        add(0x09); add(0x83) // System Wake Up
        add(0x81); add(0x60) // Input (Data, Arr, Abs, NoPreferred, NullState)
        add(0x75); add(0x06)
        add(0x95); add(0x01)
        add(0x81); add(0x03) // Input (Const, Var, Abs) — padding
        add(0xC0) // End Collection
    }

    private fun MutableList<Int>.gamepad() {
        add(0x05); add(0x01) // Usage Page (Generic Desktop)
        add(0x09); add(0x05) // Usage (Game Pad)
        add(0xA1); add(0x01) // Collection (Application)
        add(0x85); add(REPORT_ID_GAMEPAD)
        //   16 buttons
        add(0x05); add(0x09) // Usage Page (Button)
        add(0x19); add(0x01)
        add(0x29); add(0x10)
        add(0x15); add(0x00)
        add(0x25); add(0x01)
        add(0x75); add(0x01)
        add(0x95); add(0x10)
        add(0x81); add(0x02) // Input (Data, Var, Abs)
        //   Left stick X, Y; right stick Z, Rz (-127..127)
        add(0x05); add(0x01) // Usage Page (Generic Desktop)
        add(0x09); add(0x30) // X
        add(0x09); add(0x31) // Y
        add(0x09); add(0x32) // Z
        add(0x09); add(0x35) // Rz
        add(0x15); add(0x81)
        add(0x25); add(0x7F)
        add(0x75); add(0x08)
        add(0x95); add(0x04)
        add(0x81); add(0x02)
        //   Triggers Rx, Ry (0..255)
        add(0x09); add(0x33) // Rx
        add(0x09); add(0x34) // Ry
        add(0x15); add(0x00)
        add(0x25); add(0xFF)
        add(0x75); add(0x08)
        add(0x95); add(0x02)
        add(0x81); add(0x02)
        //   Hat switch (0 = up, clockwise to 7; null = 15)
        add(0x09); add(0x39) // Hat switch
        add(0x15); add(0x00)
        add(0x25); add(0x07)
        add(0x35); add(0x00) // Physical Minimum (0)
        add(0x46); add(0x3B); add(0x01) // Physical Maximum (315)
        add(0x65); add(0x14) // Unit (English rotation: degrees)
        add(0x75); add(0x04)
        add(0x95); add(0x01)
        add(0x81); add(0x42) // Input (Data, Var, Abs, NullState)
        add(0x65); add(0x00) // Unit (None)
        add(0x35); add(0x00) // Physical Minimum (0)
        add(0x45); add(0x00) // Physical Maximum (0)
        add(0x75); add(0x04)
        add(0x95); add(0x01)
        add(0x81); add(0x03) // Input (Const, Var, Abs) — padding
        add(0xC0) // End Collection
    }
}
