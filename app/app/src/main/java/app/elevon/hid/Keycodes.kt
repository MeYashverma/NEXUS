package app.elevon.hid

/**
 * USB HID usage IDs (keyboard page 0x07, consumer page 0x0C) and modifier bits.
 * Values follow the USB HID Usage Tables, which is what every OS expects.
 */
object Keycodes {
    // Modifier bits (keyboard report byte 0)
    const val MOD_LCTRL = 0x01
    const val MOD_LSHIFT = 0x02
    const val MOD_LALT = 0x04
    const val MOD_LGUI = 0x08
    const val MOD_RCTRL = 0x10
    const val MOD_RSHIFT = 0x20
    const val MOD_RALT = 0x40
    const val MOD_RGUI = 0x80

    /**
     * The Mac Fn key has no USB usage of its own; hardware keyboards report
     * Fn-key chords pre-translated. Chords that need it send the translated
     * F-key directly; MOD_LFN marks such chords for display purposes only.
     */
    const val MOD_LFN = 0x100


    const val KEY_NONE = 0x00
    const val KEY_ERROR_ROLLOVER = 0x01

    // Letters (0x04..0x1D = 'a'..'z')
    const val KEY_A = 0x04
    const val KEY_Z = 0x1D

    // Digits (0x1E..0x27 = '1'..'9','0')
    const val KEY_1 = 0x1E
    const val KEY_2 = 0x1F
    const val KEY_3 = 0x20
    const val KEY_4 = 0x21
    const val KEY_5 = 0x22
    const val KEY_6 = 0x23
    const val KEY_7 = 0x24
    const val KEY_8 = 0x25
    const val KEY_9 = 0x26
    const val KEY_0 = 0x27

    const val KEY_ENTER = 0x28
    const val KEY_ESC = 0x29
    const val KEY_BACKSPACE = 0x2A
    const val KEY_TAB = 0x2B
    const val KEY_SPACE = 0x2C
    const val KEY_MINUS = 0x2D
    const val KEY_EQUAL = 0x2E
    const val KEY_LEFTBRACKET = 0x2F
    const val KEY_RIGHTBRACKET = 0x30
    const val KEY_BACKSLASH = 0x31
    const val KEY_SEMICOLON = 0x33
    const val KEY_APOSTROPHE = 0x34
    const val KEY_GRAVE = 0x35
    const val KEY_COMMA = 0x36
    const val KEY_DOT = 0x37
    const val KEY_SLASH = 0x38
    const val KEY_CAPSLOCK = 0x39

    const val KEY_F1 = 0x3A
    const val KEY_F2 = 0x3B
    const val KEY_F3 = 0x3C
    const val KEY_F4 = 0x3D
    const val KEY_F5 = 0x3E
    const val KEY_F6 = 0x3F
    const val KEY_F7 = 0x40
    const val KEY_F8 = 0x41
    const val KEY_F9 = 0x42
    const val KEY_F10 = 0x43
    const val KEY_F11 = 0x44
    const val KEY_F12 = 0x45

    const val KEY_PRINTSCREEN = 0x46
    const val KEY_SCROLLLOCK = 0x47
    const val KEY_PAUSE = 0x48
    const val KEY_INSERT = 0x49
    const val KEY_HOME = 0x4A
    const val KEY_PAGEUP = 0x4B
    const val KEY_DELETE = 0x4C
    const val KEY_END = 0x4D
    const val KEY_PAGEDOWN = 0x4E
    const val KEY_RIGHT = 0x4F
    const val KEY_LEFT = 0x50
    const val KEY_DOWN = 0x51
    const val KEY_UP = 0x52
    const val KEY_NUMLOCK = 0x53
    const val KEY_KP_SLASH = 0x54
    const val KEY_KP_ASTERISK = 0x55
    const val KEY_KP_MINUS = 0x56
    const val KEY_KP_PLUS = 0x57
    const val KEY_KP_ENTER = 0x58
    const val KEY_KP_1 = 0x59
    const val KEY_KP_2 = 0x5A
    const val KEY_KP_3 = 0x5B
    const val KEY_KP_4 = 0x5C
    const val KEY_KP_5 = 0x5D
    const val KEY_KP_6 = 0x5E
    const val KEY_KP_7 = 0x5F
    const val KEY_KP_8 = 0x60
    const val KEY_KP_9 = 0x61
    const val KEY_KP_0 = 0x62
    const val KEY_KP_DOT = 0x63
    const val KEY_KP_EQUAL = 0x67

    // Media / consumer page (16-bit usage codes)
    const val CONSUMER_PLAY_PAUSE = 0x00CD
    const val CONSUMER_STOP = 0x00B7
    const val CONSUMER_SCAN_NEXT = 0x00B5
    const val CONSUMER_SCAN_PREVIOUS = 0x00B6
    const val CONSUMER_MUTE = 0x00E2
    const val CONSUMER_VOLUME_UP = 0x00E9
    const val CONSUMER_VOLUME_DOWN = 0x00EA
    const val CONSUMER_BRIGHTNESS_UP = 0x006F
    const val CONSUMER_BRIGHTNESS_DOWN = 0x0070
    const val CONSUMER_EJECT = 0x00B8
    const val CONSUMER_FAST_FORWARD = 0x00B3
    const val CONSUMER_REWIND = 0x00B4

    // System control report array values (see HidDescriptors.system)
    const val SYSTEM_SLEEP = 1
    const val SYSTEM_POWER = 2
    const val SYSTEM_WAKE = 3

    // Gamepad button bit positions (bit 0 = button 1 = South/A)
    const val BTN_SOUTH = 0
    const val BTN_EAST = 1
    const val BTN_WEST = 2
    const val BTN_NORTH = 3
    const val BTN_L1 = 4
    const val BTN_R1 = 5
    const val BTN_L3 = 6
    const val BTN_R3 = 7
    const val BTN_SELECT = 8
    const val BTN_START = 9
    const val BTN_GUIDE = 10

    /** Hat switch values; 15 = neutral (null state). */
    const val HAT_NEUTRAL = 15
    const val HAT_UP = 0
    const val HAT_UP_RIGHT = 1
    const val HAT_RIGHT = 2
    const val HAT_DOWN_RIGHT = 3
    const val HAT_DOWN = 4
    const val HAT_DOWN_LEFT = 5
    const val HAT_LEFT = 6
    const val HAT_UP_LEFT = 7

    fun keyForLetter(c: Char): Int? =
        if (c in 'a'..'z') KEY_A + (c - 'a') else null
}
