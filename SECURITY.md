# Security policy

Elevon is an input-injection app. Take that seriously — and take the following context with it.

## Scope

- **Android app** (`app/`): Bluetooth HID input injection, Relay GATT server + crypto, IME.
- **Relay web app** (`website/relay/`): browser client for the Relay session.

## What the app can and cannot do

- Can inject keyboard, mouse, media and gamepad input into **computers the user has explicitly
  paired with**, enforced by standard Bluetooth bonding. Unpairing closes the door.
- Cannot access the network at all — **no INTERNET permission**.
- The Relay IME can insert text into the phone's focused field, but only while the Relay
  session is active, and the UI shows a persistent RELAY ACTIVE state with a STOP control.

## Supported versions

Security fixes land on the latest release only.

## Reporting a vulnerability

Use GitHub's **private vulnerability reporting** on this repository (Security → Advisories →
Report a vulnerability). Please include:

- Affected component (app / relay web app / docs)
- Android/browser versions and a reproduction
- Impact as you understand it

You'll get an acknowledgement within **7 days**, and we'll coordinate a fix and disclosure
timeline with you. Credit in the advisory unless you prefer otherwise.

## Design notes for reviewers

- Relay's session key derives from ECDH P-256 with an out-of-band 5-digit comparison code
  (SHA-256 over both public keys) — the MITM detector. Frames are AES-256-GCM.
- The comparison code is only as strong as the user comparing it; the UI must never allow
  proceeding past a code mismatch without a warning. If you find a flow that does, that's a bug.
- HID input injection has no channel authentication beyond Bluetooth bonding itself;
  proximity attacks against unpaired advertising state are accepted risk for the Relay
  *advertisement*, but the Relay session requires encryption + code verification.
