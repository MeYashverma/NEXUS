# Elevon v0.1.1 — HID and permission fixes

**Fixes for "all controls + connection" failure on Android 16 (API 36) + Windows 11.**

This patch fixes critical bugs that made Elevon appear completely broken on Android 16 + Windows 11, while also adding light-theme screenshots and GitHub Pages deployment.

## Fixed

- **HID report sizes**: keyboard payload was 7 bytes in `onGetReport` + `sendRawKeyboard` but descriptor requires 8 bytes (mods + reserved + 6 keys). Fixed to 8 bytes with reserved byte. This broke typing, clipboard bridge, macro pad, and gamepad keyboard mode — all sent malformed reports that Windows ignored.
- **Mouse button release**: `MouseReport.payload()` returned null when idle, so button release was never sent and buttons stuck. Now always sends report, with `payloadOrNullIfIdle()` helper for idle checks.
- **Media keys (consumer)**: descriptor declares 4 bytes (usage + 2 bytes padding) but code sent 2. Fixed to 4 bytes — media prev/next/play/pause/volume now reach Windows.
- **KeyboardScreen**: tap sent press without release, leaving keys stuck on host. Now sends press then release after 12ms via coroutine, matching `ElevonSession.tapKey` pattern.
- **Android 12+ runtime permissions**: `BLUETOOTH_CONNECT`/`SCAN`/`ADVERTISE` + `POST_NOTIFICATIONS` were declared but never requested at runtime. On Android 16 (targetSdk 36) this throws `SecurityException` for `getProfileProxy`, `bondedDevices`, `connect()`. Added:
  - `MainActivity` permission launcher requesting Nearby devices + notifications on create, retrying `HidController.start()` after grant.
  - `HomeScreen` permission gate showing error card with Grant + Open settings buttons when `BLUETOOTH_CONNECT` not granted, explaining why all controls fail without it.
  - `HidController.start()` now checks permission first and surfaces OFFLINE instead of crashing, with SecurityException handling.

## Added

- Light-theme screenshots (Paper theme #FAFAF8, #F0EFEA cards, #E4571F accent, #1A1B1E text) with `prefers-color-scheme` switching via `<picture>` — dark Graphite remains default.
- GitHub Pages deployment via Actions (website/ folder, .nojekyll) — site live at https://meyashverma.github.io/NEXUS/
- Changelog page now lists both v0.1.1 and v0.1.0.

## Install

1. Download APK below — `release` for daily use, `debug` for testing. SHA-256 in `SHA256SUMS.txt`.
2. **Grant Nearby devices permission** when prompted (Android 12+). If you denied before, Home screen shows a card to grant it.
3. On computer, add Bluetooth device and pick Elevon (initiate pairing from computer).
4. Open Elevon and tap a control mode. Keyboard now properly releases keys, mouse buttons release, media keys work.

Requirements: Android 9+ (Android 16 tested) with Bluetooth HID Device profile; Windows 10/11, macOS 12+, Linux/SteamOS.

## Verify

```
sha256sum -c SHA256SUMS.txt
```

## Known limits (honest)

- XInput-only games need Steam Input (Android can't impersonate Xbox controller).
- Some OEMs disable HID profile — app detects and explains at first run.
- Relay requires Chrome/Edge, tab-scoped by browser security model.

Full docs: docs/compatibility.md, docs/troubleshooting.md, website.

**License:** Apache-2.0. No INTERNET permission, no telemetry, no accounts.
