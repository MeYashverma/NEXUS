package app.elevon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import app.elevon.ui.AppNav
import app.elevon.ui.theme.ElevonTheme

val LocalSession = compositionLocalOf<ElevonSession> {
    error("ElevonSession not provided")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val session = (application as ElevonApp).session
        val openRelay = intent?.getBooleanExtra(app.elevon.relay.RelayImeService.EXTRA_OPEN_RELAY, false) == true

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
}

