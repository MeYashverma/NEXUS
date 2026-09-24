package app.elevon.hid

/**
 * HID connection lifecycle, modelled as a plain state machine on top of
 * [android.bluetooth.BluetoothHidDevice]. The Bluetooth calls happen in
 * [HidController]; this file holds the pure state so the UI and tests can
 * reason about it without a Bluetooth stack.
 */
enum class HidConnectionState {
    /** The phone's Bluetooth HID Device profile is missing or disabled. */
    UNSUPPORTED,

    /** Bluetooth is off, or the HID app is not registered yet. */
    OFFLINE,

    /** Registered and discoverable; no host connected. */
    READY,

    /** connect() sent, waiting for the host. */
    CONNECTING,

    /** Virtual cable established with a host. */
    CONNECTED,

    /** Host went away or disconnect() was called. */
    DISCONNECTED,
}

data class HostDevice(
    val address: String,
    val name: String,
)

/**
 * Pure reducer for HID connection events. `pairingAfterUnplug` remembers a
 * fresh pairing that dropped its virtual cable so the UI can say
 * "paired — reconnecting…" instead of a scary error.
 */
class HidStateMachine {

    var state: HidConnectionState = HidConnectionState.OFFLINE
        private set
    var connectedHost: HostDevice? = null
        private set
    var pendingHost: HostDevice? = null
        private set

    val isLive: Boolean get() = state == HidConnectionState.CONNECTED

    fun onHidSupported(supported: Boolean) {
        if (!supported && state != HidConnectionState.CONNECTED) {
            state = HidConnectionState.UNSUPPORTED
        }
    }

    fun onAppRegistered() {
        if (state == HidConnectionState.OFFLINE || state == HidConnectionState.UNSUPPORTED) {
            state = HidConnectionState.READY
        }
    }

    fun onAppUnregistered() {
        if (state != HidConnectionState.OFFLINE) state = HidConnectionState.OFFLINE
        connectedHost = null
        pendingHost = null
    }

    fun onConnectRequest(host: HostDevice) {
        pendingHost = host
        state = HidConnectionState.CONNECTING
    }

    fun onConnected(host: HostDevice) {
        connectedHost = host
        pendingHost = null
        state = HidConnectionState.CONNECTED
    }

    fun onDisconnected() {
        connectedHost = null
        pendingHost = null
        when (state) {
            HidConnectionState.CONNECTED, HidConnectionState.CONNECTING ->
                state = HidConnectionState.DISCONNECTED
            else -> Unit
        }
    }

    fun onReconnectStarted() {
        state = HidConnectionState.CONNECTING
    }
}
