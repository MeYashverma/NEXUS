package app.elevon.relay

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.security.KeyPair
import java.security.interfaces.ECPublicKey
import java.util.UUID
import javax.crypto.SecretKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The phone side of Elevon Relay: a BLE GATT server the laptop's browser
 * connects to. The browser never needs anything installed — it talks Web
 * Bluetooth to this service. Frames are end-to-end encrypted after an ECDH
 * handshake (see [RelayCrypto]); the 5-digit code is verified by the user.
 *
 * This is the entire reason Relay can exist without host software, and also
 * the boundary of what it can do: the browser only sees keys typed while the
 * Relay page is focused. System-wide capture is impossible, and Elevon
 * doesn't pretend otherwise.
 */
@SuppressLint("MissingPermission") // permission state is surfaced through RelayUi
class RelayManager(
    private val context: Context,
    val bus: RelayBus,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val serviceUuid: UUID = UUID.fromString(SERVICE_UUID)
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var keyPair: KeyPair? = null
    private var sessionKey: SecretKey? = null
    private var remotePublicRaw: ByteArray? = null
    private var central: BluetoothDevice? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    // ---- lifecycle ----------------------------------------------------------

    fun start() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter
        when {
            adapter == null || !adapter.isEnabled -> {
                bus.setState(RelayState.ERROR)
                bus.update { it.copy(error = ERROR_BLUETOOTH_OFF) }
                return
            }
            !hasPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) -> {
                bus.setState(RelayState.ERROR)
                bus.update { it.copy(error = ERROR_NEEDS_PERMISSION) }
                return
            }
        }

        keyPair = RelayCrypto.generateKeyPair()
        sessionKey = null
        remotePublicRaw = null
        bus.update {
            RelayUi(state = RelayState.WAITING, code = comparisonCode())
        }

        val serverCallback = object : BluetoothGattServerCallback() {
            override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                if (status == BluetoothGatt.GATT_SUCCESS) startAdvertising(adapter)
            }

            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                when (newState) {
                    BluetoothGattServer.STATE_CONNECTED -> {
                        central = device
                        scope.launch {
                            bus.update {
                                it.copy(
                                    state = RelayState.HANDSHAKE,
                                    laptopName = device.name ?: "your laptop",
                                    error = null,
                                )
                            }
                            bus.emit(RelayEvent.Connected(device.name ?: "your laptop"))
                        }
                    }
                    BluetoothGattServer.STATE_DISCONNECTED -> {
                        central = null
                        sessionKey = null
                        remotePublicRaw = null
                        scope.launch {
                            bus.update {
                                if (it.state == RelayState.ACTIVE || it.state == RelayState.HANDSHAKE) {
                                    it.copy(state = RelayState.WAITING, laptopName = null)
                                } else {
                                    it
                                }
                            }
                            bus.emit(RelayEvent.Disconnected("link closed"))
                        }
                    }
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?,
            ) {
                if (characteristic?.uuid == CHAR_INPUT_UUID && value != null) {
                    handleFrame(value)
                }
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor?,
            ) {
                // CCCD read: report "notifications enabled" (little-endian 0x0001).
                if (descriptor?.uuid == UUID.fromString(DESCRIPTOR_CCCD_UUID)) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf(0x01, 0x00))
                } else {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf(0, 0))
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?,
            ) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }
        }

        gattServer = manager?.openGattServer(context, serverCallback)?.also { server ->
            val service = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val input = BluetoothGattCharacteristic(
                UUID.fromString(CHAR_INPUT_UUID),
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            )
            val status = BluetoothGattCharacteristic(
                UUID.fromString(CHAR_STATUS_UUID),
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ,
            )
            val cccd = BluetoothGattDescriptor(
                UUID.fromString(DESCRIPTOR_CCCD_UUID),
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE,
            )
            status.addDescriptor(cccd)
            notifyCharacteristic = status
            service.addCharacteristic(input)
            service.addCharacteristic(status)
            server.addService(service)
        }
    }

    fun stop() {
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        advertiser = null
        runCatching { gattServer?.close() }
        gattServer = null
        sessionKey = null
        remotePublicRaw = null
        central = null
        bus.update { RelayUi(state = RelayState.STOPPED) }
    }

    /** User pressed STOP; clears the typed buffer too. */
    fun stopAndClear() {
        bus.update { it.copy(buffer = "") }
        stop()
    }

    fun appendToBufferLocally(text: String) {
        bus.update { it.copy(buffer = (it.buffer + text).takeLast(4000)) }
    }

    // ---- framing ------------------------------------------------------------

    private var helloAccum: ByteArray = ByteArray(0)

    private fun handleFrame(frame: ByteArray) {
        // A 66-byte hello may arrive as one write or as 20-byte chunks
        // (browsers write conservatively). Accumulate until complete.
        if (helloAccum.isNotEmpty()) {
            helloAccum += frame
            if (helloAccum.size >= 66) {
                processHello(helloAccum)
                helloAccum = ByteArray(0)
            }
            return
        }
        when (frame[0].toInt() and 0xFF) {
            FRAME_HELLO -> {
                if (frame.size >= 66) {
                    processHello(frame)
                } else {
                    helloAccum = frame.copyOf()
                }
            }

            FRAME_DATA -> {
                val key = sessionKey ?: return
                val payload = RelayCrypto.decrypt(key, frame.copyOfRange(1, frame.size)) ?: return
                handlePlaintext(payload)
            }
        }
    }

    private fun processHello(frame: ByteArray) {
        val remote = frame.copyOfRange(1, 66)
        val mine = (keyPair?.public as? ECPublicKey)?.let { RelayCrypto.encodePublicKey(it) }
            ?: return
        remotePublicRaw = remote
        sessionKey = runCatching {
            val remoteKey = RelayCrypto.decodePublicKey(remote)
            val shared = RelayCrypto.sharedSecret(keyPair!!.private, remoteKey)
            RelayCrypto.sessionKey(shared)
        }.getOrNull()
        scope.launch {
            bus.update { it.copy(code = comparisonCode()) }
            if (sessionKey != null) {
                bus.setState(RelayState.ACTIVE)
            } else {
                bus.update { it.copy(state = RelayState.ERROR, error = ERROR_HANDSHAKE) }
            }
        }
        // Reply with our own public key so the browser can finish ECDH.
        sendToCentral(FRAME_HELLO, mine)
    }

    private fun handlePlaintext(payload: ByteArray) {
        when (payload[0].toInt() and 0xFF) {
            PAYLOAD_TEXT -> {
                val text = String(payload, 1, payload.size - 1, Charsets.UTF_8)
                if (text.isNotEmpty()) {
                    scope.launch {
                        bus.emit(RelayEvent.Text(text))
                        appendToBufferLocally(text)
                    }
                }
            }
            PAYLOAD_BACKSPACE -> {
                val n = payload.getOrNull(1)?.toInt()?.coerceIn(1, 200) ?: 1
                scope.launch { bus.emit(RelayEvent.Backspace(n)) }
            }
            PAYLOAD_ENTER -> scope.launch { bus.emit(RelayEvent.Enter) }
            PAYLOAD_END -> scope.launch { bus.emit(RelayEvent.MessageEnd) }
        }
    }

    private fun sendToCentral(type: Int, body: ByteArray) {
        val device = central ?: return
        val status = notifyCharacteristic ?: return
        val frame = byteArrayOf(type.toByte()) + body
        runCatching {
            status.value = frame
            gattServer?.notifyCharacteristicChanged(device, status, false)
        }
    }

    private fun comparisonCode(): String? {
        val mine = (keyPair?.public as? ECPublicKey)?.let { RelayCrypto.encodePublicKey(it) }
            ?: return null
        val remote = remotePublicRaw
            // Before the handshake, show the code computed over our key only;
            // it will refresh once both keys are known.
            ?: return null
        return RelayCrypto.comparisonCode(mine, remote)
    }

    // ---- advertising ----------------------------------------------------------

    private fun startAdvertising(adapter: BluetoothAdapter) {
        advertiser = adapter.bluetoothLeAdvertiser ?: run {
            scope.launch {
                bus.setState(RelayState.ERROR)
                bus.update { it.copy(error = ERROR_NO_ADVERTISER) }
            }
            return
        }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(serviceUuid))
            .setIncludeDeviceName(true)
            .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            scope.launch {
                bus.setState(RelayState.ERROR)
                bus.update { it.copy(error = ERROR_ADVERTISE_FAILED) }
            }
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val SERVICE_UUID = "f3a1e0c2-6b7d-4c8a-9e2f-0a1b2c3d4e5f"
        const val CHAR_INPUT_UUID = "f3a1e0c3-6b7d-4c8a-9e2f-0a1b2c3d4e5f"
        const val CHAR_STATUS_UUID = "f3a1e0c4-6b7d-4c8a-9e2f-0a1b2c3d4e5f"
        const val DESCRIPTOR_CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"

        const val FRAME_HELLO = 0x10
        const val FRAME_DATA = 0x11

        const val PAYLOAD_TEXT = 0x01
        const val PAYLOAD_BACKSPACE = 0x02
        const val PAYLOAD_ENTER = 0x03
        const val PAYLOAD_END = 0x04

        const val ERROR_BLUETOOTH_OFF = "bluetooth_off"
        const val ERROR_NEEDS_PERMISSION = "needs_permission"
        const val ERROR_NO_ADVERTISER = "no_advertiser"
        const val ERROR_ADVERTISE_FAILED = "advertise_failed"
        const val ERROR_HANDSHAKE = "handshake_failed"
    }
}
