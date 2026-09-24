# Roadmap

Honest buckets only. "Completed" means shipped in a release; "In progress" means being worked
on; "Planned" means genuinely scheduled; "Experimental" lives in Labs. Nothing is listed for
appearance's sake — see [research](research.md) for why several attractive ideas are *not* here.

## Completed (v0.1.0)

- All seven control modes over a single composite HID pairing
- Keyboard: compact/full/gaming/custom layouts, sticky+locked modifiers, auto-repeat, host layouts
- Touchpad: tap-to-click, two-finger scroll (V+H), press-and-hold drag, drag lock, tuning
- Gamepad: editable layouts, dead zones, sensitivity, stick clicks, hat D-pad, analog triggers,
  keyboard+mouse output mode for games without controller support
- Macro pad & custom decks: 12-slot pages, OS-aware presets, media keys, snippets, waits
- Media & presentation remotes (3 advance styles, timer, blank)
- Profiles bound to devices, per-computer OS, friendly names
- Clipboard bridge (typed to computer; local history with retention off-switch)
- Elevon Labs + Relay (BLE GATT, ECDH/AES-GCM, comparison code, RELAY ACTIVE banner, STOP)
- First-run compatibility detection with honest unsupported states
- Zero internet permission; unit tests for HID, crypto, models

## In progress

- Relay polish: reconnect flow, IME switching UX, buffer management
- Game profile presets tested per title (community-driven; contribute verified mappings)
- Accessibility pass with TalkBack users across all screens

## Planned

- Numpad mode
- Deck export/import (file-based, no cloud)
- F-Droid distribution (reproducible builds)
- More host keyboard layouts for typed text
- Pointer acceleration curves

## Experimental (Labs)

- **Relay** — current focus of Labs
- Wi-Fi Relay variant — *blocked* on a real trade-off: phone-served pages aren't secure contexts
  and the Android INTERNET permission would be needed. Documented, not shipped.
- Deck sync — undecided shape (encrypted opt-in vs file export)

## Deliberately not planned

- Screen mirroring/streaming — requires host software, violating the core principle
- File transfer — same; KDE Connect already does this well
- Console support — cryptographically blocked by console vendors
- Xbox/XInput impersonation — Android does not expose vendor/product IDs
- Voice input to computer — needs either host software or network services

Change is tracked per release in the [changelog](changelog.md).
