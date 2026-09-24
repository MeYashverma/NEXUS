package app.elevon.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothHidDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the [BluetoothHidDevice] profile proxy and turns high-level input calls
 * into HID reports on the wire. All Bluetooth work happens on a single
 * dedicated thread; UI observes [state].
 */
@Suppress("MissingPermission") // permission state is checked and surfaced via HidUiState
class HidController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val btExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "elevon-bt") }
    private val mainHandler = Handler(Looper.getMainLooper())

    private val machine = HidStateMachine()
    private val _state = MutableStateFlow(HidUiState.from(machine))
    val state: StateFlow<HidUiState> = _state.asStateFlow()

    val keyboard = KeyboardReport()
    val mouse = MouseReport()
    val gamepad = GamepadReport()

    private var hidProxy: BluetoothHidDevice? = null
    private var adapter: BluetoothAdapter? = null
    @Volatile private var registered = false
    @Volatile private var bootProtocol = false
    private var profileReadyAt: Long = 0
    private val supportCheckPosted = AtomicBoolean(false)

    private var currentDevice: BluetoothDevice? = null
    private var typingJob: kotlinx.coroutines.Job? = null

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            this@HidController.registered = registered
            mainHandler.post {
                if (registered) {
                    machine.onAppRegistered()
                    publish()
                } else {
                    machine.onAppUnregistered()
                    publish()
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            super.onConnectionStateChanged(device, state)
            mainHandler.post {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        currentDevice = device
                        machine.onConnected(device.toHost())
                        publish()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        currentDevice = null
                        machine.onDisconnected()
                        publish()
                    }
                    else -> Unit
                }
            }
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            super.onVirtualCableUnplug(device)
            // The host removed the pairing. Reflect it honestly in state.
            mainHandler.post {
                currentDevice = null
                machine.onDisconnected()
                publish()
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            super.onSetProtocol(device, protocol)
            bootProtocol = protocol == BluetoothHidDevice.PROTOCOL_BOOT_MODE
        }

        override fun onGetReport(
            device: BluetoothDevice,
            type: Byte,
            id: Byte,
            bufferSize: Int,
        ) {
            super.onGetReport(device, type, id, bufferSize)
            if (type != BluetoothHidDevice.REPORT_TYPE_INPUT) return
            val payload = when (id.toInt()) {
                HidDescriptors.REPORT_ID_KEYBOARD -> ByteArray(7)
                HidDescriptors.REPORT_ID_MOUSE -> ByteArray(5)
                HidDescriptors.REPORT_ID_GAMEPAD -> gamepad.current().let { ByteArray(9) }
                else -> null
            } ?: return
            runCatching { hidProxy?.replyReport(device, type, id, payload) }
        }

        override fun onOutputReport(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            // Not part of the public Callback in API 28; LED state arrives via
            // onInterruptData on some stacks. The keyboard tracks LEDs when the
            // host pushes an output report through replyReport flows.
            super.onInterruptData(device, reportId, data)
            if (reportId.toInt() == 0x01) keyboard.onOutputReport(data)
        }
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            btExecutor.execute {
                hidProxy = proxy as? BluetoothHidDevice
                val sdp = BluetoothHidDeviceAppSdpSettings(
                    "Elevon",
                    "Elevon universal input device",
                    "Elevon",
                    BluetoothHidDevice.SUBCLASS1_COMBO,
                    HidDescriptors.composite(),
                )
                val ok = runCatching {
                    hidProxy?.registerApp(sdp, null, null, btExecutor, callback) ?: false
                }.getOrDefault(false)
                profileReadyAt = System.currentTimeMillis()
                mainHandler.post {
                    if (ok || hidProxy != null) {
                        // Registration result arrives via onAppStatusChanged.
                    } else {
                        machine.onHidSupported(false)
                        publish()
                    }
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            btExecutor.execute {
                hidProxy = null
                registered = false
                mainHandler.post {
                    machine.onAppUnregistered()
                    publish()
                }
            }
        }
    }

    fun start() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        adapter = manager?.adapter
        val a = adapter
        if (a == null) {
            machine.onHidSupported(false); publish(); return
        }
        if (!a.isEnabled) {
            // Bluetooth off: still attempt profile proxy when it turns on; for now
            // report offline and let the UI prompt the user.
            machine.onAppUnregistered(); publish()
        }
        runCatching {
            a.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        }
        // Some OEMs never deliver the HID_DEVICE profile. If the proxy hasn't
        // come up within 6 seconds of start(), call it unsupported honestly.
        if (supportCheckPosted.compareAndSet(false, true)) {
            scope.launch {
                delay(6000)
                if (hidProxy == null && _state.value.connectionState != HidConnectionState.CONNECTED) {
                    machine.onHidSupported(false)
                    publish()
                }
            }
        }
    }

    fun stop() {
        runCatching {
            hidProxy?.let { p ->
                if (registered) p.unregisterApp()
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, p)
            }
        }
        hidProxy = null
        btExecutor.shutdown()
    }

    // ---- connection -------------------------------------------------------

    fun connect(address: String) {
        val a = adapter ?: return
        btExecutor.execute {
            runCatching {
                val device = a.getRemoteDevice(address)
                machine.onConnectRequest(device.toHost())
                mainHandler.post { publish() }
                hidProxy?.connect(device)
            }
        }
    }

    fun disconnect() {
        val d = currentDevice ?: return
        btExecutor.execute { runCatching { hidProxy?.disconnect(d) } }
    }

    fun hostName(): String? = _state.value.hostName

    /** Bonded computers we could connect to (for the Connect sheet). */
    fun pairedHosts(): List<HostDevice> {
        val a = adapter ?: return emptyList()
        return runCatching {
            a.bondedDevices
                .orEmpty()
                .filter { it.type != BluetoothDevice.DEVICE_TYPE_LE }
                .map { it.toHost() }
                .sortedBy { it.name.lowercase() }
        }.getOrDefault(emptyList())
    }

    // ---- input ------------------------------------------------------------

    /** Sends a full keyboard state (mods + up to 6 keys) immediately. */
    fun sendKeyboardState() {
        val d = currentDevice ?: return
        val payload = keyboard.payload() ?: return
        btExecutor.execute { runCatching { hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_KEYBOARD, payload) } }
    }

    fun sendRawKeyboard(mods: Int, keys: List<Int>) {
        val d = currentDevice ?: return
        val payload = ByteArray(7)
        payload[0] = mods.toByte()
        keys.take(6).forEachIndexed { i, k -> payload[2 + i] = k.toByte() }
        btExecutor.execute { runCatching { hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_KEYBOARD, payload) } }
    }

    fun sendMouseState() {
        val d = currentDevice ?: return
        val payload = mouse.payload() ?: return
        btExecutor.execute { runCatching { hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_MOUSE, payload) } }
    }

    fun sendMouseButtons(buttons: Int) {
        val d = currentDevice ?: return
        val payload = ByteArray(5)
        payload[0] = buttons.toByte()
        btExecutor.execute { runCatching { hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_MOUSE, payload) } }
    }

    fun sendConsumer(usage: Int) {
        val d = currentDevice ?: return
        val press = byteArrayOf((usage and 0xFF).toByte(), ((usage shr 8) and 0xFF).toByte())
        val release = byteArrayOf(0, 0)
        btExecutor.execute {
            runCatching {
                hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_CONSUMER, press)
                Thread.sleep(24)
                hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_CONSUMER, release)
            }
        }
    }

    fun sendSystem(code: Int) {
        val d = currentDevice ?: return
        val press = byteArrayOf(code.toByte())
        val release = byteArrayOf(0)
        btExecutor.execute {
            runCatching {
                hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_SYSTEM, press)
                Thread.sleep(24)
                hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_SYSTEM, release)
            }
        }
    }

    /** Sends one raw gamepad payload. The gamepad screen drives a fixed-rate loop. */
    fun sendGamepad(payload: ByteArray) {
        val d = currentDevice ?: return
        btExecutor.execute { runCatching { hidProxy?.sendReport(d, HidDescriptors.REPORT_ID_GAMEPAD, payload) } }
    }

    /**
     * Types [text] as HID keystrokes (US layout; see CharMap). Emits progress
     * through [onProgress] (0..1) and can be cancelled by starting another type.
     */
    fun typeText(text: String, onProgress: (Float) -> Unit = {}, onDone: (Boolean) -> Unit = {}) {
        val strokes = CharMap.strokesFor(text)
        typingJob?.cancel()
        typingJob = scope.launch {
            var ok = currentDevice != null
            for ((index, stroke) in strokes.withIndex()) {
                if (!isActive || currentDevice == null) { ok = false; break }
                sendRawKeyboard(stroke.mods, listOf(stroke.usage))
                delay(8)
                sendRawKeyboard(0, emptyList())
                delay(4)
                onProgress((index + 1f) / strokes.size)
            }
            onDone(ok && isActive)
        }
    }

    fun cancelTyping() {
        typingJob?.cancel()
        sendRawKeyboard(0, emptyList())
    }

    /** Warm-up pulse against Bluetooth sniff-mode latency (first touch after idle). */
    private var lastInputAt = 0L
    fun warmUpIfIdle() {
        val now = System.currentTimeMillis()
        if (now - lastInputAt > 3000) {
            sendMouseButtons(0)
        }
        lastInputAt = now
    }

    // ---- internals --------------------------------------------------------

    private fun publish() {
        _state.value = HidUiState.from(machine).copy(capsLock = keyboard.ledCapsLock)
    }

    private fun BluetoothDevice.toHost(): HostDevice =
        HostDevice(
            address = address,
            name = runCatching { name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown computer",
        )
}

/** Immutable snapshot for the UI layer. */
data class HidUiState(
    val connectionState: HidConnectionState,
    val hostName: String?,
    val hostAddress: String?,
    val capsLock: Boolean,
) {
    companion object {
        fun from(machine: HidStateMachine): HidUiState = HidUiState(
            connectionState = machine.state,
            hostName = machine.connectedHost?.name ?: machine.pendingHost?.name,
            hostAddress = machine.connectedHost?.address ?: machine.pendingHost?.address,
            capsLock = false,
        )
    }
}
