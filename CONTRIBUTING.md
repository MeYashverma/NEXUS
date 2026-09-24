# Contributing to Elevon

Thanks for helping build the missing input device. This project has a few firm rules — they're
what make the product trustworthy, so they're enforced in review.

## Ground rules (non-negotiable)

1. **No host software for core features.** Nothing in `app/` may depend on a desktop app,
   server, driver or helper for keyboard, touchpad, gamepad, decks, media or presentation.
   Features that need computer-side cooperation must live in Labs, be labeled Experimental,
   and be documented honestly.
2. **No new permissions without written justification.** The PRIVACY story is that the manifest
   is short and auditable. Any PR adding a permission must explain it in the PR description and
   update `docs/privacy.md`. PRs adding the INTERNET permission will be declined.
3. **No fake features, no overclaiming.** If a game/OS/browser can't do something, the UI and
   docs say so. Compatibility claims need a table row, not a marketing sentence.
4. **No telemetry, ever.** No analytics, crash reporting, or network beacons — see rule 2.
5. **Public APIs only.** No reflection into Android internals, no hidden-API workarounds
   (they're what killed older projects on Android 11/12 — see the research).

## Getting set up

```bash
git clone https://github.com/MeYashverma/NEXUS.git
cd NEXUS/app
./gradlew assembleDebug testDebugUnitTest
```

JDK 17, Android SDK 36. Open `app/` in Android Studio if you prefer.

## Where help is wanted

- **Real-device testing** — especially phones reported without the HID Device profile
  (LG, some OnePlus/Motorola/Nokia/Fairphone). Issue reports from real hardware are gold.
- **Game profiles** — presets with verified mappings (output mode noted honestly per game).
- **Host layouts** — typed-text tables beyond US/UK/DE/FR.
- **Accessibility** — TalkBack and switch-access review of all screens.
- **Docs & website** — clarity beats cleverness.

## Pull requests

- Keep PRs small and focused; one feature or fix each.
- Touching HID, crypto, or JSON models? Extend the unit tests in
  `app/app/src/test/` — descriptor parsing, report packing, and the relay handshake have tests.
- Update docs when behaviour changes. The website is generated:
  edit `tools/pages_source.py`, run `python3 tools/build_site.py`, commit both.
- Follow the existing style (Kotlin official style; plain language in user-facing strings).

## Reporting bugs

Open an issue with:

- Phone model + Android version (and whether the HID compatibility check passed)
- Computer OS and how it's connected (Bluetooth version helps)
- Elevon version (About screen)
- What you did, what you expected, what happened

Screenshots of the connection state beat logs. If you have `adb logcat` output, attach it as a
file rather than pasting a wall of text.

## Security

See [SECURITY.md](SECURITY.md). Please don't open public issues for security problems.

## Code of conduct

By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md). Be decent.
