# Elevon v0.2.0 — Premium E-wing, fullscreen, Labs expansion

**Premium redesign + new control modes + fullscreen landscape.**

## Added — Logo
- **New E-wing mark**: slanted geometric E that reads as swept wing + letter E. Single path, sharp angular ends, instrument-grade. Replaces lame chevron-wing. Updated everywhere: adaptive launcher foreground, monochrome themed icon, favicon, website header/footer, branding SVGs, design.md. Concepts explored via AI (V and E-wing) in branding/logo/new-logo-concept*.png.

## Added — Fullscreen landscape
- **Gamepad**: tap fullscreen icon → locks to landscape, hides system bars (immersive), pad surface fills entire screen, floating back/edit/exit. Ideal for gaming. Also auto-uses fullscreen when system is landscape. Edit mode still works: drag to move, tap to resize.
- **Keyboard**: fullscreen button locks to landscape, hides bars, keys fill screen with larger touch targets (weight 1f). Rotate button toggles portrait/landscape. Works for Full, Compact, Gaming, Custom. Sticky/locked modifiers visible. Great for gaming and long typing.
- **Touchpad**: fullscreen landscape with curve label visible, immersive.
- **Numpad**: fullscreen landscape for spreadsheets.
- **Gyro Mouse**: fullscreen landscape for couch.

## Added — Labs features (real implementations)
- **Numpad mode**: new ControlMode.NUMPAD, NumpadScreen.kt with KP keycodes (0x54-0x63, 0x67: KP_SLASH, ASTERISK, MINUS, PLUS, ENTER, 1-9, 0, DOT, EQUAL), fullscreen, rotate. Core, not experimental. Great for Excel, calculators.
- **Gyro Mouse (Labs)**: new ControlMode.GYRO, GyroMouseScreen.kt using SensorManager TYPE_GYROSCOPE, converts yaw/pitch to mouse deltas with sensitivity 0.2-3x, tap to enable, hold to click, left/right/scroll buttons. Experimental, fullscreen landscape, with honest explanation.
- **Pointer acceleration curves**: new enum AccelerationCurve (LINEAR, EASE_OUT, EASE_IN_OUT, PRECISE, GAMING) with descriptions. Setting in Settings → Controls and Touchpad tuning. Applied in TouchpadScreen via applyCurve() with sqrt/smoothstep logic.
- **Deck export/import**: in Macro Pad / Custom surface TopAppBar, export button shares JSON via ACTION_SEND (all pages), import button pastes JSON and upserts decks with new IDs. File-based, no cloud, honest. No INTERNET needed.
- **LabsScreen expansion**: real cards for Gyro, Numpad, Pointer curves, Deck export/import (Core) plus planned Wi-Fi Relay, Deck sync, Laser pointer with honest notes.

## Added — Settings
- SettingsRepository: pointerCurve (StringPref), gyroMode (StringPref), gyroSensitivity (FloatPref), numpadLayout.
- SettingsScreen: Controls section shows curve chips with description, Gyro Mouse section with mode chips and sensitivity slider.
- Version bump: versionCode 3, versionName 0.2.0.

## Fixed
- Keycodes: added KP_* constants for numpad.
- AppNav: added routes mode/numpad and mode/gyro, HomeScreen now shows 9 modes with Calculate and Sensors icons.
- Website: updated header/footer SVGs to new E-wing, premium redesign kept, announcement bar updated, changelog includes v0.2.0.
- Broken download links fixed in v0.1.1 remain fixed.

## Install
1. Download APK from Releases — release for daily, debug for testing. SHA256 in SHA256SUMS.txt.
2. Grant Nearby devices permission (Android 12+).
3. Pair from computer Bluetooth settings.
4. Try new modes: Numpad, Gyro Mouse, Gamepad fullscreen, Keyboard fullscreen.

Requirements: Android 9+ (Android 16 tested) with Bluetooth HID Device profile; Windows 10/11, macOS 12+, Linux/SteamOS.

## Verify
```
sha256sum -c SHA256SUMS.txt
```

## Known limits
- XInput-only games need Steam Input.
- Some OEMs disable HID profile.
- Gyro Mouse accuracy varies by phone gyroscope.
- Relay tab-scoped by browser security.

Full docs: website, docs/.

**License:** Apache-2.0. No INTERNET permission for core, no telemetry, no accounts.
