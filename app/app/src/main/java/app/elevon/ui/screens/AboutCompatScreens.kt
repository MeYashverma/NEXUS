package app.elevon.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.elevon.ui.components.StateCard

/** In-app About: mirrors the website's about page. */
@Composable
fun AboutScreen(nav: NavHostController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        TopAppBar(
            title = { Text("About Elevon") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
        )
        Text("Elevon", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Your phone. Your controls.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Elevon turns an Android phone into a real Bluetooth input device: keyboard, touchpad, gamepad, macro decks, media and presentation remotes — for any computer, with nothing to install on it. One pairing unlocks every mode.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        Text("0.1.0 · open source · Apache-2.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        StateCard(
            title = "Privacy",
            body = "No INTERNET permission — the OS blocks any network use. No accounts, no tracking, no analytics. Devices, profiles, layouts and clipboard history never leave this phone. Full details on the website's Privacy page.",
        )
        Spacer(Modifier.height(10.dp))
        StateCard(
            title = "Credits",
            body = "Built on Android's public Bluetooth HID Device and GATT APIs. Design tokens and research live in the repository. The name: an elevon is a control surface that does the job of two.",
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** In-app compatibility + troubleshooting summary; the website has the long version. */
@Composable
fun CompatScreen(nav: NavHostController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        TopAppBar(
            title = { Text("Compatibility") },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
        )
        CompatBlock(
            "This phone",
            listOf(
                "Needs Android 9 (2018) or newer with the Bluetooth HID Device profile.",
                "Most phones from recent years have it. Some (notably certain LG, older OnePlus, Motorola and Fairphone models) ship with it switched off — if so, Elevon tells you upfront instead of failing silently.",
                "Relay advertising needs Bluetooth LE advertising, present on effectively all phones from this era.",
            ),
        )
        CompatBlock(
            "Computers",
            listOf(
                "Windows 10/11: keyboard, mouse, touchpad, media keys — yes. Gamepad works in DirectInput/Raw Input games and through Steam Input; XInput-only games need Steam.",
                "macOS: keyboard, mouse, touchpad, media keys — yes. Gamepad is visible to generic-HID apps (Steam, emulators, browsers), not to Apple's GameController framework.",
                "Linux / SteamOS: everything works, gamepad included.",
                "iPadOS/iOS: keyboard and (iPadOS 13.4+) pointer yes; gamepad no. Android TV/Google TV: yes, including gamepad.",
            ),
        )
        CompatBlock(
            "Known limits — stated plainly",
            listOf(
                "The gamepad is a generic HID gamepad. It cannot impersonate an Xbox controller because Android doesn't let apps choose the USB/Bluetooth vendor or product ID.",
                "BIOS/boot-level keyboard control is not guaranteed: some pre-boot environments only speak the boot protocol.",
                "Typed text over Bluetooth uses the layout set on the computer. US/UK map fully; German/French map base keys, with AltGr/dead-key characters documented as limited.",
                "Wired USB HID is impossible without root on stock Android — not offered.",
            ),
        )
        CompatBlock(
            "If something doesn't work",
            listOf(
                "Nothing happens when connecting: unpair on BOTH sides, then pair again from the computer while Elevon is open on this phone.",
                "Inputs feel laggy after a pause: that's Bluetooth power saving — the first touch warms the link; keep this screen open while playing.",
                "The computer also grabs audio: some computers connect the phone's audio profiles too. Disable that device in the computer's sound settings.",
                "Keys look wrong on a German/French computer: set the host layout in Settings → Keyboard.",
            ),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CompatBlock(title: String, bullets: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        bullets.forEach {
            Row(Modifier.padding(vertical = 3.dp)) {
                Text("•  ", color = MaterialTheme.colorScheme.primary)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}
