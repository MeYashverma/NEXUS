package app.elevon.relay

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.elevon.R
import app.elevon.hid.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The Elevon Relay keyboard: a deliberately minimal IME that inserts text
 * arriving from the laptop into whatever field is focused. It shows a
 * prominent RELAY ACTIVE banner and a big STOP at all times — the user must
 * always be able to see that keystrokes are being redirected, and kill it
 * instantly (see docs/privacy.md).
 *
 * When Relay is off, the view explains how to start it and offers to switch
 * back to the user's normal keyboard.
 */
class RelayImeService : InputMethodService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var stateView: TextView
    private lateinit var bufferView: TextView
    private lateinit var stopButton: Button
    private lateinit var switchButton: Button
    private lateinit var startButton: Button

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.elevon_background_dark))
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        stateView = TextView(this).apply {
            text = "RELAY INACTIVE"
            setTextColor(0xFFF2C14E.toInt())
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(stateView, LinearLayout.LayoutParams(-1, -2))

        bufferView = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFA9AEB6.toInt())
            maxLines = 2
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(bufferView, LinearLayout.LayoutParams(-1, -2))

        stopButton = Button(this).apply {
            text = "STOP RELAY"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFB3261E.toInt())
        }
        stopButton.setOnClickListener {
            ServiceLocator.relayManager(this@RelayImeService).stopAndClear()
            switchToNextInputMethod(false)
        }
        root.addView(stopButton, LinearLayout.LayoutParams(-1, dp(56)))

        startButton = Button(this).apply {
            text = "Start Relay in Elevon"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF2E3238.toInt())
        }
        startButton.setOnClickListener {
            val intent = Intent(this, app.elevon.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_OPEN_RELAY, true)
            startActivity(intent)
        }
        root.addView(startButton, LinearLayout.LayoutParams(-1, dp(48)))

        switchButton = Button(this).apply {
            text = "Use my normal keyboard"
            setTextColor(0xFFECEDEE.toInt())
            setBackgroundColor(0x00000000)
        }
        switchButton.setOnClickListener { switchToNextInputMethod(false) }
        root.addView(switchButton, LinearLayout.LayoutParams(-1, dp(44)))

        observe()
        return root
    }

    private fun observe() {
        val manager = ServiceLocator.relayManager(this)
        scope.launch {
            manager.bus.ui.collect { ui ->
                stateView.text = when (ui.state) {
                    RelayState.ACTIVE -> "RELAY ACTIVE — connected to ${ui.laptopName ?: "your laptop"}"
                    RelayState.HANDSHAKE -> "RELAY CONNECTING — ${ui.laptopName ?: "laptop"} · code ${ui.code ?: "·····"}"
                    RelayState.WAITING -> "RELAY WAITING — open elevon.app/relay on your laptop"
                    RelayState.OFF, RelayState.STOPPED, RelayState.ERROR -> "RELAY INACTIVE"
                }
                val active = ui.state == RelayState.ACTIVE || ui.state == RelayState.HANDSHAKE
                stopButton.visibility = if (active) View.VISIBLE else View.GONE
                startButton.visibility = if (active) View.GONE else View.VISIBLE
                switchButton.visibility = if (active) View.VISIBLE else View.GONE
                bufferView.text = if (ui.buffer.isEmpty()) "" else "Buffer: …${ui.buffer.takeLast(60)}"
            }
        }
        scope.launch {
            manager.bus.events.collect { event ->
                val ic = currentInputConnection ?: return@collect
                when (event) {
                    is RelayEvent.Text -> ic.commitText(event.text, 1)
                    is RelayEvent.Backspace -> {
                        if (event.count == 1) {
                            ic.deleteSurroundingText(1, 0)
                        } else {
                            ic.deleteSurroundingText(event.count, 0)
                        }
                    }
                    RelayEvent.Enter -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    RelayEvent.MessageEnd -> Unit
                    is RelayEvent.Connected, is RelayEvent.Disconnected -> Unit
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val ui = ServiceLocator.relayManager(this).bus.ui.value
        stateView.text = when (ui.state) {
            RelayState.ACTIVE -> "RELAY ACTIVE — connected to ${ui.laptopName ?: "your laptop"}"
            RelayState.HANDSHAKE -> "RELAY CONNECTING — ${ui.laptopName ?: "laptop"}"
            RelayState.WAITING -> "RELAY WAITING — open elevon.app/relay on your laptop"
            RelayState.OFF, RelayState.STOPPED, RelayState.ERROR -> "RELAY INACTIVE"
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OPEN_RELAY = "open_relay"

        fun openRelayIntent(context: Context): Intent =
            Intent(context, app.elevon.MainActivity::class.java).putExtra(EXTRA_OPEN_RELAY, true)
    }
}
