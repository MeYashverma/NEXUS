package app.elevon.relay

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Relay state machine + event bus. The GATT server ([RelayManager]) pushes
 * events in; the Relay screen and the Relay keyboard ([RelayImeService])
 * consume them.
 */
enum class RelayState {
    OFF,
    /** Advertising, waiting for the laptop to connect. */
    WAITING,
    /** GATT connected, handshake in progress. */
    HANDSHAKE,
    /** Encrypted session live. */
    ACTIVE,
    /** Terminal condition with a user-facing reason in [RelayUi.error]. */
    ERROR,
    /** Stopped by the user. */
    STOPPED,
}

data class RelayUi(
    val state: RelayState = RelayState.OFF,
    val laptopName: String? = null,
    val code: String? = null,
    val buffer: String = "",
    val error: String? = null,
)

sealed class RelayEvent {
    data class Connected(val laptopName: String) : RelayEvent()
    data class Disconnected(val reason: String) : RelayEvent()
    data class Text(val text: String) : RelayEvent()
    data class Backspace(val count: Int) : RelayEvent()
    data object Enter : RelayEvent()
    data object MessageEnd : RelayEvent()
}

class RelayBus {

    private val _ui = MutableStateFlow(RelayUi())
    val ui: StateFlow<RelayUi> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<RelayEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<RelayEvent> = _events.asSharedFlow()

    fun update(transform: (RelayUi) -> RelayUi) {
        _ui.value = transform(_ui.value)
    }

    fun setState(state: RelayState) {
        _ui.value = _ui.value.copy(state = state)
    }

    suspend fun emit(event: RelayEvent) {
        _events.emit(event)
    }
}
