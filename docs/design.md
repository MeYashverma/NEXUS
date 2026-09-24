# Elevon design system

This is the single source of truth for how Elevon looks and behaves. The Android app, the website and the brand assets all follow it. If something here conflicts with a nicer idea in the moment, change this file first, then the code.

> Direction: **an instrument, not a utility.** Elevon should feel like a well-made piece of hardware — think test equipment, a mixer, a flight deck — not like a neon gaming app or a generic settings screen.

## Brand

| Token | Value |
| --- | --- |
| Name | **Elevon** (always capitalised E, one word) |
| Feature names | **Elevon Relay**, **Elevon Labs** |
| Tagline | **Your phone. Your controls.** |
| Secondary line | One pairing. Every control. Nothing to install. |
| Voice | Calm, plain, confident. Short sentences. Never hype. Say "no" plainly when something isn't supported. |

An *elevon* is a control surface that does the job of two. That idea — one surface, many jobs — is the whole product.

## Logo

The mark is a **premium E-wing**: a slanted, geometric E that reads as a swept wing and as the letter E. One continuous path, sharp angular ends, instrument-grade precision. The E does the job of two — letter and wing — just like an elevon does the job of two control surfaces. Flat colour #FF7A45 on #101114, with subtle glow allowed on website for depth. The previous chevron-wing was replaced in v0.2 for being too generic.

| Asset | File |
| --- | --- |
| App icon (adaptive foreground) | `app/src/main/res/mipmap-anydpi-v26/ic_launcher_foreground.xml` (E-wing path) |
| Logo SVG | `branding/logo/elevon-logo.svg` (E-wing + wordmark) |
| Monogram | `branding/logo/elevon-glyph.svg` (E-wing on rounded square) |
| Social preview | `branding/social/github-social.png` (1280×640, E-wing) |
| Favicon | `website/assets/favicon.svg` + `.ico` (E-wing) |
| Concepts | `branding/logo/new-logo-concept*.png` (AI explorations, V and E-wing) |

## Colour

One signal hue. Everything else is neutral. The accent is reserved for *live* things: the connected state, active keys, record-like actions. It never decorates.

| Token | Dark (Graphite) | Light (Paper) | Use |
| --- | --- | --- | --- |
| `surface` | `#101114` | `#FAFAF8` | App background |
| `surfaceContainer` | `#17191D` | `#F0EFEA` | Cards, panels |
| `surfaceHigh` | `#1E2126` | `#E6E4DE` | Keycaps at rest |
| `outline` | `#2E3238` | `#D5D2CA` | Hairlines, key borders |
| `onSurface` | `#ECEDEE` | `#1A1B1E` | Primary text |
| `onSurfaceVariant` | `#A9AEB6` | `#5B6068` | Secondary text |
| `accent` | `#FF7A45` | `#E4571F` | Live state, active keys, primary buttons |
| `accentContainer` | `#3A241A` | `#FFE0D2` | Accent-tinted containers |
| `success` | `#7AD98B` | `#1E7F35` | Connected |
| `warning` | `#F2C14E` | `#9A6B00` | Connecting, limited |
| `danger` | `#F26D6D` | `#B3261E` | Disconnected, destructive |
| `keyPressed` | accent at 100% | accent at 100% | Key press feedback (colour + scale, not glow) |

Rules:

- **Contrast.** All text pairs meet WCAG AA at minimum; body text targets AAA. `onSurfaceVariant` on `surface` is ≥ 4.5:1 in both themes.
- **Dark first.** Graphite is the default theme; Paper follows `system`.
- **No gradients, no glass, no neon.** Depth comes from two elevation levels and hairline borders.
- **Charts/diagrams** on the website use the same palette, `#FF7A45` sparingly.

## Type

- Android: **Roboto Flex** variable (system fallback Roboto); weights 400/500/700. Tabular figures for timers and counters.
- Website: **Space Grotesk** (headings) + **Inter** (body), self-hosted in `website/assets/fonts/` so the site works offline. Roboto for code/keys.
- Scale (website, desktop): 12 / 14 / 16 / 20 / 28 / 40 / 64. Line-height 1.5 body, 1.1 display.
- Keycaps use 500 weight, slightly condensed letter-spacing (+0.02em on small caps labels).

## Shape & elevation

- Corner radius: 8 (small chips), 14 (keycaps), 20 (cards), 28 (sheets).
- Elevation: level 0 flat; level 1 = 1px border + subtle shadow (dark: black 40%, light: black 8%). That's all. No higher levels.
- Keycaps: 14dp radius, 1dp `outline` border, pressed state = border → accent + 0.97 scale + haptic. Release restores. Locked modifiers (Caps Lock) show a filled accent dot.

## Iconography

Material Symbols (Rounded) on both app and website. Stroke weight 400, filled only for active states. No emoji in UI chrome. (App icon import pending — see docs/TODO.)

## Spacing & layout

- 4dp grid. Screen margins 20dp. Control-mode screens go **edge to edge**; chrome floats.
- Touch targets ≥ 48dp always; keyboard keys ≥ 44dp height in compact, ≥ 56 in full.
- Landscape: control surfaces keep a 16:9-ish safe area; nothing important under camera cutouts (`WindowInsets.safeDrawing`).

## Motion

- Durations: press feedback 40ms in / 80ms out. Screen transitions 220ms, decelerate.
- Anything that moves more than 8dp respects the system **reduce-motion** setting.
- Connection state changes animate a small dot, never a full-screen takeover.

## Sound & haptics

- Haptics: `VIRTUAL_KEY` for taps, `LONG_PRESS` for holds, `CONFIRM` for connect/success. Off by default on keycaps, on for mode switches (user setting).
- No UI sounds in v1 (Bluke's switch sounds are a known delight; noted in roadmap, not copied).

## Accessibility

- Every key and control has a `contentDescription`/`aria-label`.
- All state is announced ("Connected to Gaming PC", "Caps Lock on").
- Min contrast AA; focus rings visible in light and dark.
- Control size slider (0.8×–1.3×) on gamepad and keyboard.
- The touchpad exposes left/right-click and scroll as explicit buttons for switch access.

## Writing rules

- Buttons are verbs: **Connect**, **Stop relay**, **Forget device**.
- Errors follow the pattern: plain title → what to do → Try again / Troubleshoot / Learn more. Never stack traces, never "Error code 0x…".
- Feature labels carry an honesty chip: **Core** · **Experimental** (Labs) · **Limited** with a one-line reason.
