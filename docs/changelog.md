# Changelog

Versioning: [semver](https://semver.org/). Each release ships signed APKs with SHA-256
checksums on the [Releases page](https://github.com/MeYashverma/NEXUS/releases).

## 0.1.1 — HID and permission fixes · September 2026

Fixes for "all controls + connection" failure on Android 16 (API 36) + Windows 11.

### Fixed

- **HID report sizes**: keyboard payload was 7 bytes in two places (onGetReport + sendRawKeyboard)
  but descriptor requires 8 bytes (mods + reserved + 6 keys). Fixed to 8 bytes with reserved byte.
  This broke typing, clipboard bridge, macro pad, and gamepad keyboard mode — all sent malformed
  reports that Windows ignored. Also fixed mouse button release not being sent (MouseReport.payload()
  returned null on release, leaving buttons stuck).
- **Consumer (media keys) report**: descriptor declares 4 bytes (usage + padding) but code sent 2.
  Fixed to 4 bytes; media/prev/next/play/pause/volume now work on Windows.
- **KeyboardScreen**: tap sent press without release, leaving keys stuck on host. Now sends
  press, then release after 12ms via coroutine, matching ElevonSession.tapKey pattern.
- **Android 12+ runtime permissions**: BLUETOOTH_CONNECT/SCAN/ADVERTISE + POST_NOTIFICATIONS were
  declared but never requested at runtime. On Android 16 (targetSdk 36) this throws
  SecurityException for getProfileProxy, bondedDevices, connect(). Added:
  - MainActivity permission launcher requesting Nearby devices + notifications on create,
    retrying HidController.start() after grant.
  - HomeScreen permission gate showing error card with Grant + Open settings buttons when
    BLUETOOTH_CONNECT not granted, explaining why all controls fail without it.
  - HidController.start() now checks permission first and surfaces OFFLINE instead of crashing,
    with SecurityException handling.

### Added

- Light-theme screenshots (Paper theme) with prefers-color-scheme switching via <picture>
  (already in 0.1.1 website build).

## 0.1.0 — first public alpha · September 2026

First public release. Research-backed rebuild of the working concept (formerly "NEXUS",
renamed — see [naming research](research.md#naming-research)).

### Added

- **Bluetooth HID core** — composite descriptor (keyboard + mouse + consumer + system +
  gamepad) so one pairing unlocks every mode; 6-key-rollover keyboard with host LED tracking;
  throttled mouse with AC Pan; packed gamepad with hat switch; boot-protocol replies;
  warm-up pulse against Bluetooth sniff-mode latency.
- **Keyboard mode** — compact, full, gaming, custom layouts; sticky (tap) and locked (hold)
  modifiers; function row toggle; auto-repeat; host layout setting (US/UK/DE/FR).
- **Touchpad mode** — edge-to-edge surface; tap-to-click; two-finger tap right-click;
  two-finger scroll (vertical + horizontal); press-and-hold drag with drag lock; pointer/scroll
  speed, natural scrolling; explicit L/M/R buttons.
- **Gamepad mode** — drag-to-position layout editor with per-element sizing; radial dead zones;
  sensitivity; stick clicks via double-tap; analog triggers; profiles per game; honest dual
  output: native HID gamepad **or** keyboard+mouse (works in games with no controller support).
- **Macro Pad & Custom decks** — 12-slot pages; OS-aware shortcut presets (Cmd vs Ctrl);
  media keys; typed snippets; wait steps; edit-by-long-press.
- **Media & Presentation remotes** — big targets; hold-to-repeat volume/seek; three advance-key
  styles; count-up timer; blank key.
- **Profiles & devices** — per-computer preferred profile; OS per device; friendly names;
  five default profiles.
- **Clipboard bridge** — phone→computer by typing; local 20-item history; retention off-switch.
- **Elevon Labs** —
  **Relay**: phone-side BLE GATT server, ECDH P-256 + HKDF + AES-GCM session crypto,
  5-digit comparison code, dedicated Relay IME with persistent RELAY ACTIVE banner and STOP,
  browser client at [/relay](https://meyashverma.github.io/NEXUS/relay/).
- **Privacy posture** — no INTERNET permission; short auditable permission list.
- **Product infrastructure** — research report, design system, full docs, 17-page website,
  CI-built APK artifacts, community templates.

### Known issues

- XInput-only games don't see the gamepad without Steam Input (platform limitation —
  [compatibility](compatibility.md)).
- Relay requires Chromium-based browsers; Linux needs a Chrome flag.
- Phones with manufacturer-disabled HID profiles cannot be fixed app-side; Elevon detects and
  explains at first run.
- Typed accents via AltGr/dead keys are limited on non-US host layouts; use Relay for such text.

### Upgrade notes

First release. If you paired an earlier internal build, unpair both sides once before pairing
the release build (descriptor changed during development).
