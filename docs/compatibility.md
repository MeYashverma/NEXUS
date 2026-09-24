# Compatibility

> In-app summary: **About → Compatibility**. Website version:
> [meyashverma.github.io/NEXUS/compatibility.html](https://meyashverma.github.io/NEXUS/compatibility.html).
> This page is the full reference with reasons.

## Your phone (the Elevon device)

| Requirement | Status | Detail |
| --- | --- | --- |
| Android 9.0 (API 28), August 2018 | **Required** | `BluetoothHidDevice` shipped in 9. |
| Bluetooth HID Device profile **enabled** | Usually yes | Some manufacturers ship it disabled: certain LG, older OnePlus (5T/6 era), Motorola, Nokia, and Fairphone 6 are reported. Elevon detects this at first run and states it. It cannot be fixed app-side. |
| Bluetooth LE advertising (Relay only) | Standard | Needed to advertise the Relay GATT service. |
| Root | Never | Nothing in Elevon needs or uses root. |
| Google Play services | Not required | No dependency. |

Known behaviours:

- **One HID app at a time.** Android allows a single app to register the HID Device profile.
  If another input-emulator app holds it, Elevon reports the failure instead of fighting over it.
- **Descriptors cache at pairing time.** If you switch the HID profile (Settings → Advanced,
  compatibility descriptor) or upgrade from a build with a different descriptor, **unpair on
  both sides once** and pair again. The app warns you when this applies.

## Your computer (the host)

| Host | Keyboard | Mouse / touchpad | Media keys | Gamepad | Decks (as keys) |
| --- | :-: | :-: | :-: | :-: | :-: |
| Windows 10 / 11 | ✅ | ✅ | ✅ | ◐ | ✅ |
| macOS 10.12+ | ✅ | ✅ (pointer on 10.15+ is smoothest) | ✅ | ◐ | ✅ |
| Linux (BlueZ 5.50+) | ✅ | ✅ | ✅ | ✅ | ✅ |
| SteamOS / Steam Deck | ✅ | ✅ | ✅ | ✅ | ✅ |
| ChromeOS | ✅ | ✅ | ✅ | ◐ app-dependent | ✅ |
| iPadOS / iOS | ✅ (pointer: iPadOS 13.4+) | ◐ | ◐ | ❌ | ✅ |
| Android TV / Google TV | ✅ | ✅ | ✅ | ✅ mostly | ✅ |
| Android phones/tablets as host | ◐ vendor-dependent | ◐ | ◐ | ◐ | ◐ |
| Consoles (PS/Xbox/Switch) | ❌ | ❌ | ❌ | ❌ | ❌ |

### Gamepad details per host

- **Windows:** a generic HID gamepad appears in `joy.cpl`, DirectInput, Raw Input and
  Windows.Gaming.Input. **XInput-only games don't see it** — launch through Steam with Steam
  Input enabled, or use in-game remapping. This is a platform wall: Android doesn't let apps
  choose the Bluetooth vendor/product ID, so Xbox impersonation is impossible.
- **macOS:** visible to generic-HID consumers — Steam, SDL games, emulators, browsers'
  Gamepad API. Apple's GameController framework filters to known controllers, so native Mac
  games using it won't list Elevon.
- **Linux:** `hid-generic` → evdev/joydev; SDL and Steam handle it natively.
- **iOS/iPadOS:** Apple accepts generic HID *keyboards* but not generic gamepads. Keyboard and
  decks work; gamepad doesn't.

## Typed text and keyboard layouts

Typing over HID sends key *usage codes*; the computer maps them through **its** configured
layout. Elevon's "host layout" setting compensates:

| Host layout | Typed-text accuracy |
| --- | --- |
| US (default) | ✅ Complete |
| UK | ✅ Complete |
| German (QWERTZ) | ◐ Base keys correct; AltGr characters and dead-key accents (ä/ö/ü/ß via AltGr) type as nearest base character |
| French (AZERTY) | ◐ Base keys correct; accented characters (é/è/ç) limited as above |

Text sent through **Relay commits as text** (IME `commitText`) and is not affected by layouts —
another reason long non-ASCII text belongs in Relay.

## Relay browsers (laptop side)

| Browser | Works | Note |
| --- | :-: | --- |
| Chrome / Edge — Windows 10 1703+, macOS, ChromeOS | ✅ | Recommended |
| Chrome / Edge — Linux | ◐ | Needs `chrome://flags/#enable-experimental-web-platform-features`; BlueZ 5.43+ |
| Opera / Vivaldi desktop | ◐ | Web Bluetooth listed but historically broken on desktop |
| Brave | ◐ | Blocks Web Bluetooth by default |
| Firefox / Safari | ❌ | Web Bluetooth not implemented, no plans |

The phone side of Relay works on any supported phone (BLE advertising).

## Feature honesty labels

- **Core** — works with nothing installed on the computer, on every ✅/◐ host above.
- **Experimental** (Labs) — Relay and friends; installs nothing; may change.
- **Limited** — specific, documented walls: XInput, Apple GameController, consoles,
  boot-protocol environments (BIOS-level keyboard control isn't guaranteed; some pre-boot
  environments only speak the boot protocol), wired USB HID (impossible without root; not
  offered), AltGr/dead-key typed characters.

## Related

- [Troubleshooting](troubleshooting.md) for practical fixes.
- [Research](research.md#platform-capabilities-and-hard-limits) for the platform evidence.
