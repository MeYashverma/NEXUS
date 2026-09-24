# Elevon v0.1.0 — first public build

**Your phone. Your controls. One pairing. Every control. Nothing to install.**

Elevon turns an Android phone into a real Bluetooth keyboard, touchpad, gamepad,
macro pad, media/presentation remote and clipboard bridge for any computer that
already speaks Bluetooth HID — no desktop app, no driver, no helper, no account.

## What works in this build

- **Keyboard** — compact / full / gaming layouts, US + UK host layouts, sticky
  modifiers with double-tap lock, auto-repeat, optional function row, editable
  custom layout.
- **Touchpad** — tap-to-click, two-finger scroll, natural scrolling, drag lock,
  pointer/scroll speed, two/three-finger gestures.
- **Gamepad** — real HID gamepad (buttons, dual sticks, D-pad, triggers with a
  drag-to-position editable layout), plus an honest "keyboard & mouse" output
  mode for games that only accept XInput (documented per-platform in
  `docs/compatibility.md`).
- **Macro pad & custom decks** — 12-slot pages of shortcuts, media keys, text
  and delays; ships with streaming/video-call/meeting presets.
- **Media & presentation remotes** — big targets for away-from-desk use.
- **Profiles** — per-computer preferences (control mode, layouts, gamepad map).
- **Clipboard bridge** — send your phone clipboard to the computer as typed
  keystrokes, with on-device history you can clear.
- **NEXUS Labs → Elevon Relay (experimental)** — type from your laptop's browser
  onto the phone over Web Bluetooth, end-to-end encrypted with a 5-digit
  comparison code. Browser support is honestly scoped in-app and on the website.
- Onboarding you can skip, grouped settings, dark "Graphite" / light "Paper"
  themes, haptics control, and no tracking of any kind.

## Install

1. Download an APK below — `release` for daily use, `debug` for testing.
   SHA-256 checksums are in `SHA256SUMS.txt`.
2. On the computer, add a Bluetooth device and pick **Elevon** (initiate
   pairing from the **computer** — the app never asks for location).
3. Open Elevon and tap a control mode.

Requirements: Android 9+ with Bluetooth; Windows 10+, macOS 12+, or a
Linux/SteamOS stack with BlueZ. Per-mode platform notes — including the honest
limits on iOS, consoles and OEM HID stacks — live in
[docs/compatibility.md](../blob/main/docs/compatibility.md).

## Verify

```
sha256sum -c SHA256SUMS.txt
```

## Known limits (by design, not by accident)

- iOS and game consoles do not accept this kind of composite HID device.
- Some OEM Bluetooth stacks (noted in the compatibility doc) only surface the
  keyboard/mouse reports.
- German/French host layouts type the base layer over HID; AltGr/dead-key
  characters are not guaranteed — Relay text mode is layout-independent.
- Relay cannot and will not intercept a laptop's keyboard system-wide; that is
  the browser's security model, documented verbatim.

Full details: [docs/compatibility.md](../blob/main/docs/compatibility.md),
[docs/troubleshooting.md](../blob/main/docs/troubleshooting.md),
[website](../tree/main/website).

**License:** Apache-2.0. No telemetry, no accounts, no network permission.
