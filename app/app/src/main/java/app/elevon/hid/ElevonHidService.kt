package app.elevon.hid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.elevon.MainActivity
import app.elevon.R

/**
 * Foreground service that keeps the HID virtual cable alive while a computer
 * is connected, and shows the "connected to <host>" notification with a
 * Disconnect action so the user is always in control.
 */
class ElevonHidService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            ServiceLocator.hidController(this).disconnect()
            stopSelf()
            return START_NOT_STICKY
        }
        val host = intent?.getStringExtra(EXTRA_HOST) ?: ""
        startInForeground(host)
        return START_STICKY
    }

    private fun startInForeground(host: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_hid),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.notif_channel_hid_desc) }
            manager.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val disconnect = PendingIntent.getService(
            this, 1,
            Intent(this, ElevonHidService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_elevon)
            .setContentTitle(getString(R.string.notif_connected_title, host.ifBlank { "computer" }))
            .setContentText(getString(R.string.notif_connected_text))
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, getString(R.string.notif_action_disconnect), disconnect)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "elevon_hid"
        const val NOTIFICATION_ID = 41
        const val EXTRA_HOST = "host"
        const val ACTION_DISCONNECT = "app.elevon.hid.DISCONNECT"

        fun start(context: Context, host: String) {
            val intent = Intent(context, ElevonHidService::class.java)
                .putExtra(EXTRA_HOST, host)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ElevonHidService::class.java))
        }
    }
}

/** Tiny service locator (the app deliberately avoids a DI framework). */
object ServiceLocator {
    @Volatile private var hidControllerInstance: HidController? = null
    @Volatile private var relayManagerInstance: app.elevon.relay.RelayManager? = null

    fun hidController(context: Context): HidController =
        hidControllerInstance ?: synchronized(this) {
            hidControllerInstance ?: HidController(context.applicationContext).also {
                hidControllerInstance = it
            }
        }

    fun relayManager(context: Context): app.elevon.relay.RelayManager =
        relayManagerInstance ?: synchronized(this) {
            relayManagerInstance ?: app.elevon.relay.RelayManager(
                context.applicationContext,
                app.elevon.relay.RelayBus(),
            ).also { relayManagerInstance = it }
        }
}
