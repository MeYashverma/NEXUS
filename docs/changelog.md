# Changelog

Versioning: [semver](https://semver.org/). Each release ships signed APKs with SHA-256
checksums on the [Releases page](https://github.com/MeYashverma/NEXUS/releases).

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
