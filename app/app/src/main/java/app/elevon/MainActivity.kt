package app.elevon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import app.elevon.ui.AppNav
import app.elevon.ui.theme.ElevonTheme

val LocalSession = compositionLocalOf<ElevonSession> {
    error("ElevonSession not provided")
}

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val session = (application as? ElevonApp)?.session
        val allGranted = results.values.all { it }
        if (allGranted) {
            // Retry HID registration now that Nearby devices permission is granted.
            session?.hid?.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val session = (application as ElevonApp).session
        val openRelay = intent?.getBooleanExtra(app.elevon.relay.RelayImeService.EXTRA_OPEN_RELAY, false) == true

        // Android 12+ (API 31) introduced runtime Nearby-devices permissions for Bluetooth.
        // Android 13+ (API 33) added POST_NOTIFICATIONS for the foreground notification.
        // On Android 16 (API 36, targetSdk 36) enforcement is strict — without these,
        // getProfileProxy, bondedDevices, connect() all throw SecurityException.
        // Request them up-front so first-run pairing works.
        requestNearbyPermissionsIfNeeded()

        setContent {
            val theme by session.settings.theme.collectAsState()
            ElevonTheme(themeMode = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(LocalSession provides session) {
                        val nav = rememberNavController()
                        AppNav(navController = nav, openRelayDirectly = openRelay)
                    }
                }
            }
        }
    }

    private fun requestNearbyPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) needed.add(Manifest.permission.BLUETOOTH_SCAN)
            if (!hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)) needed.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
