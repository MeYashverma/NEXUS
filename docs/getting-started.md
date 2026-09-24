# Getting started

> The five-minute version lives on the [website](../website/getting-started.html). This page is
> the complete reference. Nothing here requires installing anything on your computer — that's
> the point of the product.

## Requirements

| What | Minimum | Notes |
| --- | --- | --- |
| Phone | Android 9.0 (2018) with Bluetooth | Bluetooth HID Device profile must be enabled by the manufacturer (most phones: yes) |
| Computer | Anything with Bluetooth that accepts HID keyboards/mice | Windows 10+, macOS, Linux, ChromeOS, Android TV, iPadOS (keyboard) |
| Permissions | Nearby devices (Bluetooth) + notifications | Never location. Never internet. |
| Computer software | **None** | For the core experience, ever |

## Install

1. Download `elevon-x.y.z.apk` from [Releases](https://github.com/MeYashverma/NEXUS/releases).
2. (Recommended) verify: `sha256sum elevon-x.y.z.apk` and compare with the checksum published on
   the release.
3. Install. Android asks about "unknown apps" for any APK outside a store — that warning is about
   the install channel, not about Elevon. Every release is built publicly by CI.
4. Or build from source: `git clone` → `cd app` → `./gradlew assembleDebug`.

## First run

Elevon runs a **compatibility check** before anything else:

- **Bluetooth available?** If off, you'll be asked to turn it on.
- **HID Device profile present?** If your phone maker disabled it, Elevon says so plainly and
  explains that nothing can fix it app-side. No silent buttons, ever.

Then it asks for the Bluetooth ("Nearby devices") permission. That's the only sensitive
permission, and it's the one that lets the phone *be* a Bluetooth device. Elevon never asks for
location, contacts, storage, or network.

## Pair a computer (one time per computer)

1. Open Elevon. From Home, tap **Connect** → **Pair a new computer** if your phone isn't
   discoverable yet (Android 12+ makes the phone discoverable through the pairing flow).
2. On the computer: **Bluetooth settings → Add device** and select your phone's name.
3. Accept the pairing prompt on the phone.
4. The computer now shows a keyboard + mouse + gamepad named after your phone. On Windows you'll
   find it under *Bluetooth & devices*; on macOS under *Bluetooth*; on Linux under your BT applet.

> **Why pair from the computer?** On Android 9–11 the app can't reliably scan for devices without
> location permission — which Elevon refuses to request. Starting from the computer avoids that
> entirely and is faster anyway.

## Connect and choose a control

Back in Elevon, your paired computer appears in the Connect panel. Tap **Connect**. When the
status pill turns green ("Connected"), pick a mode:

- **Keyboard** — layouts via the chips at the top; hold a modifier to lock it.
- **Touchpad** — the whole screen is the surface; the sliders icon tunes speed.
- **Gamepad** — profiles row picks a layout; the pencil icon edits it.
- **Macro Pad / Custom** — decks of big buttons; long-press a button to edit.
- **Media / Presentation** — big remotes.

Switching modes never re-pairs: the same Bluetooth connection carries every mode.

## Reconnecting later

Open Elevon → **Devices** → tap **Connect** on the friendly-named computer. Set a **preferred
profile** per device so the right controls (and OS-aware shortcuts) apply automatically when
you connect.

## Clipboard

- **Phone → computer:** Clipboard page → pick an item → "Type on computer". It types the text as
  keystrokes at the cursor.
- **Computer → phone:** via [Relay](../website/relay/index.html)'s paste box (experimental).
- Retention is off-switchable; history is capped at 20 items and never leaves the phone.

## If something doesn't work

Start with [troubleshooting](troubleshooting.md) — most issues are a half-removed pairing on one
side or a computer that also connected the phone's audio profile.
