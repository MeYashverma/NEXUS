# Privacy

> Website version: [privacy.html](https://meyashverma.github.io/NEXUS/privacy.html).

Elevon's privacy story is a **capability**, not a policy: the app manifest contains **no
INTERNET permission**, so the Android OS blocks every form of network access. Data
exfiltration isn't against the rules — it's impossible.

## What data exists, and where it lives

| Data | Where | Survives uninstall? |
| --- | --- | --- |
| Paired computers (address, name, OS guess, last-used) | App-private storage on the phone | No |
| Profiles (mode, layouts, gamepad/deck choices) | App-private storage | No |
| Gamepad layouts, custom keyboard layouts | App-private storage | No |
| Clipboard history (max 20 items, off-switchable) | App-private storage | No |
| Relay session state (keys, recent buffer) | Memory; buffer cleared on STOP | No |
| Anything in the cloud | **Does not exist** | — |

## What crosses the air

- **Bluetooth HID input reports** (keystrokes, mouse deltas, gamepad state, media keys) — sent
  only to computers you have paired with, under standard Bluetooth bonding encryption.
- **Relay sessions** (experimental): typed text between your browser and your phone, wrapped in
  E2E encryption (ECDH P-256 → HKDF-SHA256 → AES-256-GCM) with a 5-digit comparison code for
  man-in-the-middle detection. The Relay web page is a static file: no server, no logging,
  no analytics.

## What Elevon never does

- No accounts, no sign-in, no identifiers.
- No analytics, crash reporting, or usage statistics (there is no network capability).
- No background clipboard reading — clipboard features run only while you use them.
- No location permission, ever, on any Android version.
- No advertising ID, no third-party SDKs. The dependency list is AndroidX + Kotlin coroutines,
  auditable in `app/gradle/libs.versions.toml`.

## Permissions, one by one

| Permission | Why |
| --- | --- |
| `BLUETOOTH_CONNECT` (12+) / `BLUETOOTH` (≤11) | Act as an HID device; talk to paired hosts. |
| `BLUETOOTH_SCAN` with `neverForLocation` (12+) | Discover/pair during connect flows. |
| `BLUETOOTH_ADVERTISE` (12+) | Relay advertising + discoverability on 12+. |
| `BLUETOOTH_ADMIN` (≤11) | Legacy roles on older Android. |
| `FOREGROUND_SERVICE` (+ `connectedDevice` type) | Keep the HID virtual cable alive while in use. |
| `POST_NOTIFICATIONS` | The "Connected to X · Disconnect" notification. |
| `VIBRATE` | Optional haptics. |
| ~~`INTERNET`~~ | **Not present.** |

## Deleting your data

- Forget devices: **Devices → trash icon**.
- Clear clipboard: **Clipboard → Delete all history**, or disable retention.
- Everything else: **uninstall the app**. There is no server holding anything back.

## Relay safety rules

- A persistent **RELAY ACTIVE — connected to: …** banner is visible on both the Relay screen and
  the Relay keyboard whenever a session exists.
- **STOP RELAY** clears the buffer and tears the session down; closing the browser tab does the
  same from the laptop side.
- The 5-digit code must match on both screens. Mismatch ⇒ stop. The UI never lets a session
  proceed past an unverified code silently.
