# Troubleshooting

> Also on the website: [troubleshooting.html](https://meyashverma.github.io/NEXUS/troubleshooting.html).
> Ordered by how often each fix resolves the problem.

## Connection

### The computer doesn't see the phone / pairing fails

1. Open Elevon and leave it in the foreground during pairing.
2. **Remove old pairings on BOTH sides.** A half-removed pairing (deleted on the computer but
   not the phone, or vice versa) is the single most common cause. Phone: Settings → Bluetooth →
   gear icon → Forget. Computer: Bluetooth settings → remove device.
3. Pair again, initiated from the **computer**, and accept the prompt on the phone.
4. If Elevon's first-run check said your phone lacks the HID profile, no pairing trick will help —
   the manufacturer disabled it (see [compatibility](compatibility.md)).

### Connected, but inputs do nothing

- Confirm the Home screen pill says **Connected** (green).
- Some computers also connect the phone's **audio profiles** and get confused. On the computer,
  remove/disable the phone as an audio input/output device (keep it as an input device).
- Toggle the phone's Bluetooth off and on, then reconnect from Elevon.
- Another input-emulator app may hold the HID profile. Close it (or uninstall), then reconnect.

### Keeps disconnecting

- Keep the Elevon screen on while controlling (automatic in control modes).
- Distance and interference: 2.4 GHz Wi-Fi, microwaves and USB 3 ports near the computer's
  Bluetooth antenna all hurt. Try within a metre first.
- Aggressive battery savers (common on Chinese OEM ROMs) may kill Bluetooth apps in the
  background. Allow Elevon to run while in use; Elevon deliberately does **not** request
  battery-optimisation exemptions.

### First input after a pause is late

Bluetooth drops idle links into a low-power "sniff mode". Elevon warms the link when you touch a
control, so this should be a subtle effect. If it's noticeable, give the surface a sacrificial
tap after any pause.

## Input feel

### Cursor too fast / slow, scrolling wrong direction

Touchpad → tuning icon: pointer speed, scroll speed, natural scrolling, tap-to-click, drag lock.

### Keys type wrong characters

Set **Settings → Keyboard → host layout** to match the computer's configured layout. US/UK are
complete; German/French map base keys with accents documented as limited. (For long non-ASCII
text, use Relay, which commits text directly.)

### Gamepad buttons map oddly in a specific game

Generic HID pads land with different default orders per game engine. Remap once in-game, or let
Steam Input normalise it. If a control genuinely does nothing (e.g. a D-pad direction missing),
that's a bug — [file it](https://github.com/MeYashverma/NEXUS/issues) with the game and host.

### Sticks feel drift-y or twitchy

Raise the profile's **dead zone** slider (Gamepad → profile chip row). Lower it for racing.

## Decks & macros

### A macro did nothing

Macros only send while **connected**. Also check the deck button's subtitle: text snippets type
over the US/host layout, so a snippet is verified for US/UK; media keys work on all hosts.

### "Switch app" on a Mac pressed the launcher

OS-aware shortcuts come from the **device's OS setting** (Devices → pencil → computer type) or
the active profile. Set it to macOS for Cmd-based chords.

## Relay

### The laptop's picker doesn't show the phone

- Start Relay on the phone first (Labs → Relay → Start) and keep the screen on.
- Use Chrome/Edge; Firefox and Safari don't support Web Bluetooth.
- Linux: enable `chrome://flags/#enable-experimental-web-platform-features`, ensure BlueZ ≥ 5.43.
- Bluetooth on, phone not connected elsewhere as an input device.

### Keys don't arrive on the phone

Relay inserts text only where the phone is looking for it:

1. Enable the **Elevon Relay** keyboard: phone Settings → System → Keyboards → Manage on-screen
   keyboards → enable Elevon Relay. Switch to it (it shows a RELAY ACTIVE banner).
2. Or watch Relay's buffer on the Relay screen and copy from there.
3. The phone must show **RELAY ACTIVE**. If it shows the pairing code still, complete the
   comparison — typing is blocked until codes are verified.

### Relay paused itself

Deliberate: switching tabs or minimising the browser pauses capture so your typing can't be
collected invisibly. Click back into the Relay page to resume, or press STOP RELAY to end.

### The codes don't match

Stop immediately and start over. A mismatch means the secure channel isn't with the device you
think it is. Never continue past an unverified code.

## Still stuck?

[Open an issue](https://github.com/MeYashverma/NEXUS/issues) with phone model + Android version,
computer OS, Elevon version, and what the connection pill showed. Device
[compatibility reports](https://github.com/MeYashverma/NEXUS/issues/new?template=compat_report.md)
are especially valuable.
