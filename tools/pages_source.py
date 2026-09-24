"""All Elevon website pages. Executed by build_site.py (imports page())."""

from build_site import NAV, REPO, VERSION, build_page

# ---------------------------------------------------------------- helpers ---

CHIPS = {
    "core": '<span class="chip core">Core</span>',
    "exp": '<span class="chip experimental">Experimental</span>',
    "lim": '<span class="chip limited">Limited</span>',
}


def hero(
    kicker: str,
    title: str,
    lede: str,
    actions: str = "",
    trust: list[str] | None = None,
) -> str:
    trust_html = (
        f'<div class="trust">{"".join(f"<span>{t}</span>" for t in trust)}</div>'
        if trust
        else ""
    )
    return f"""
<section class="wrap hero">
  <div class="kicker">{kicker}</div>
  <h1>{title}</h1>
  <p class="lede">{lede}</p>
  <div class="actions">{actions}</div>
  {trust_html}
</section>"""


def cta(label: str, href: str, primary: bool = False) -> str:
    return f'<a class="btn {"primary" if primary else "outline"}" href="{href}">{label}</a>'


# ------------------------------------------------------------------ index ---

INDEX = hero(
    kicker="Your phone. Your controls.",
    title="One pairing.<br>Every control.",
    lede=(
        "Elevon turns your Android phone into a real wireless keyboard, touchpad, gamepad, "
        "macro deck and remote for your computer — a standard Bluetooth device, like a proper peripheral. "
        "Your computer needs <strong>nothing installed</strong>."
    ),
    actions=cta("Get the app", f"{REPO}releases", True)
    + cta("See how it works", "how-it-works.html")
    + cta("Try Relay", "relay/index.html"),
    trust=[
        "No desktop companion app",
        "No account",
        "No internet permission",
        "Open source (Apache-2.0)",
    ],
) + f"""
<section class="wrap block tight">
  <div class="shot"><img src="assets/shot-hero.png" alt="Elevon home screen showing a connected computer and the seven control modes" loading="lazy"></div>
</section>
<section class="wrap block">
  <h2>Seven controls. Zero software on your computer.</h2>
  <div class="grid four">
    <div class="card"><div class="icon">⌨</div><h3>Keyboard</h3><p>Compact, full, gaming and your own custom layout — with real modifier keys and function row.</p></div>
    <div class="card"><div class="icon">☝</div><h3>Touchpad</h3><p>Move, tap to click, two-finger scroll, press-and-hold drag. Tuned pointer and scroll speeds.</p></div>
    <div class="card"><div class="icon">🎮</div><h3>Gamepad</h3><p>Sticks, D-pad, triggers, bumpers. Editable layouts, dead zones, per-game profiles.</p></div>
    <div class="card"><div class="icon">▦</div><h3>Macro Pad</h3><p>Pages of big one-tap buttons: shortcuts, media keys, text snippets. No desktop server needed.</p></div>
    <div class="card"><div class="icon">▶</div><h3>Media</h3><p>Play, volume, mute, seek — large controls for the couch.</p></div>
    <div class="card"><div class="icon">.present</div><h3>Presentation</h3><p>Slides, blank screen, timer. Reads like a proper clicker, not a hack.</p></div>
    <div class="card"><div class="icon">✦</div><h3>Custom</h3><p>Build your own control surface: streaming deck, editing deck, anything.</p></div>
    <div class="card"><div class="icon">↔</div><h3>Profiles</h3><p>Work laptop, gaming PC, living room. Each remembers its mode, layout and deck.</p></div>
  </div>
</section>
<section class="wrap block">
  <h2>How is this possible without host software?</h2>
  <p>Elevon registers itself with Android's Bluetooth HID Device profile — the same standard every wireless
  keyboard and mouse has used for decades. Your computer sees a normal keyboard, mouse and gamepad.
  Nothing to install, nothing running in the tray, no drivers, no firewall prompts.</p>
  <div class="grid three">
    <div class="card"><h3>Pair once</h3><p>Add the phone from your computer's Bluetooth settings, exactly like a wireless keyboard.</p>{CHIPS['core']}</div>
    <div class="card"><h3>Switch modes instantly</h3><p>One pairing covers every mode — the same connection becomes a gamepad or a deck the moment you tap it.</p>{CHIPS['core']}</div>
    <div class="card"><h3>Explore Labs</h3><p>Relay types from your laptop keyboard onto your phone, from the browser. Clearly experimental.</p>{CHIPS['exp']}</div>
  </div>
</section>
<section class="wrap block">
  <h2>What Elevon refuses to pretend</h2>
  <p>Every remote app oversells something. Here, the limits are on the box:</p>
  <div class="grid two">
    <div class="card"><h3>XInput games {CHIPS['lim']}</h3><p>The gamepad is a genuine generic HID controller — but Android never lets it impersonate an Xbox pad, so XInput-only games need Steam Input. Controller-aware games and Steam just work.</p></div>
    <div class="card"><h3>Some phones {CHIPS['lim']}</h3><p>A few manufacturers ship Bluetooth HID switched off. Elevon checks honestly at first run and tells you — it never shows a button that does nothing.</p></div>
    <div class="card"><h3>Relay is tab-scoped {CHIPS['exp']}</h3><p>A web page can only see keys typed while it is focused. System-wide keyboard capture is impossible from a browser — Relay says so instead of faking it.</p></div>
    <div class="card"><h3>No clipboard spying {CHIPS['core']}</h3><p>Clipboard sharing works by typing, and through Relay's paste box. Watching a computer's clipboard would need host software — so Elevon doesn't do it.</p></div>
  </div>
</section>
<section class="wrap block">
  <h2>Private by construction</h2>
  <p>Elevon's Android app has <strong>no INTERNET permission</strong>. Your operating system physically
  prevents it from sending anything anywhere. No account. No analytics. No telemetry. Devices, profiles,
  layouts and clipboard history live on your phone and nowhere else.</p>
  <div class="actions">{cta("Read the privacy page", "privacy.html")}</div>
</section>
"""

# --------------------------------------------------------------- features ---

FEATURES = hero(
    kicker="Features",
    title="Everything a pocket input device should be",
    lede="One pairing, seven control modes, deep customization — and honest labels everywhere about what works where.",
    actions=cta("Get started", "getting-started.html", True),
) + """
<section class="wrap block">
  <div class="feature">
    <div>
      <h2>Keyboard</h2>
      <div class="tag-row">{core}</div>
      <p>A keyboard built for controlling a computer, not for texting. Full desktop rows when you need them,
      a one-handed compact layout when you don't, a gaming cluster for WASD titles, and a custom layout
      you design yourself.</p>
      <ul>
        <li>Real modifiers: Ctrl, Alt, Win/Cmd, Shift — tap for sticky, hold to lock</li>
        <li>Function row, navigation cluster, Esc, Tab, Caps Lock with host LED sync</li>
        <li>Auto-repeat on hold, haptic feedback options</li>
        <li>Host keyboard layout setting so text lands correctly on non-US computers</li>
      </ul>
    </div>
    <div class="shot"><img src="assets/shot-keyboard.png" alt="Elevon keyboard mode with the full desktop layout" loading="lazy"></div>
  </div>
  <div class="feature flip">
    <div>
      <h2>Touchpad</h2>
      <div class="tag-row">{core}</div>
      <p>The whole screen is the surface. One finger moves the cursor, two fingers scroll vertically and
      horizontally, a quick tap clicks, a two-finger tap right-clicks, press-and-hold drags with drag lock.</p>
      <ul>
        <li>Adjustable pointer speed, scroll speed, natural scrolling</li>
        <li>Tap-to-click and drag lock toggles</li>
        <li>Explicit left / middle / right buttons for precision work</li>
        <li>Keep-screen-on while you're controlling</li>
      </ul>
    </div>
    <div class="shot"><img src="assets/shot-touchpad.png" alt="Elevon touchpad mode, edge-to-edge gesture surface with tuning panel" loading="lazy"></div>
  </div>
  <div class="feature">
    <div>
      <h2>Gamepad</h2>
      <div class="tag-row">{core} {lim}</div>
      <p>Sticks with adjustable dead zones, an eight-way D-pad, analog triggers, bumpers, stick clicks,
      start/select/guide. Move and resize every control, and save profiles per game.</p>
      <ul>
        <li>Two output modes, honestly labeled: real gamepad, or keyboard &amp; mouse</li>
        <li>Keyboard &amp; mouse mode works in games with <em>no</em> controller support — even Minecraft Java</li>
        <li>Presets for controller games, racing, and keyboard-era games</li>
        <li>Steam and SDL games see a standard generic HID controller</li>
      </ul>
      <p><a href="controller.html">Full gamepad story →</a></p>
    </div>
    <div class="shot"><img src="assets/shot-gamepad.png" alt="Elevon gamepad mode with sticks, D-pad, face buttons and triggers" loading="lazy"></div>
  </div>
  <div class="feature flip">
    <div>
      <h2>Macro Pad &amp; Custom surfaces</h2>
      <div class="tag-row">{core}</div>
      <p>Pages of twelve big buttons. Copy, paste, undo, screenshot, Alt+Tab, show desktop, lock, media
      keys, your own text snippets and waits. Everything runs over standard keyboard and media keys —
      unlike Stream Deck Mobile or Macro Deck, there is no desktop server to install.</p>
      <ul>
        <li>OS-aware: Copy is Ctrl+C on Windows and Cmd+C on a Mac</li>
        <li>Multiple pages; starter decks for streaming, editing, music, presenting</li>
        <li>Long-press any button to edit it</li>
      </ul>
    </div>
    <div class="shot"><img src="assets/shot-macros.png" alt="Elevon macro pad with a grid of large action buttons" loading="lazy"></div>
  </div>
  <div class="feature">
    <div>
      <h2>Media &amp; Presentation</h2>
      <div class="tag-row">{core}</div>
      <p>Media: transport, volume, mute and seek with big touch targets for the couch. Presentation:
      giant next/previous buttons, F5 start, Esc end, a B-key blackout, and a count-up timer —
      with a choice of arrow keys, Page keys, or N/P advance to match your software.</p>
    </div>
    <div class="shot"><img src="assets/shot-media.png" alt="Elevon media remote with large transport and volume controls" loading="lazy"></div>
  </div>
  <div class="feature flip">
    <div>
      <h2>Profiles &amp; devices</h2>
      <div class="tag-row">{core}</div>
      <p>Profiles bundle a mode, keyboard layout, gamepad profile and deck page — Work Laptop opens the
      touchpad with Mac shortcuts, Gaming PC opens the gamepad with your layout. Bind a profile to a
      computer and it applies automatically when you connect.</p>
    </div>
    <div class="shot"><img src="assets/shot-profiles.png" alt="Elevon profiles list with per-computer defaults" loading="lazy"></div>
  </div>
  <div class="feature">
    <div>
      <h2>Elevon Relay</h2>
      <div class="tag-row">{exp}</div>
      <p>The reverse direction: your laptop's keyboard types on your phone, through the browser,
      end-to-end encrypted with a code you compare. Nothing installed on the laptop.</p>
      <p><a href="relay/index.html">Read about Relay →</a></p>
    </div>
    <div class="shot"><img src="assets/shot-relay.png" alt="Elevon Relay page showing the pairing code and RELAY ACTIVE banner" loading="lazy"></div>
  </div>
</section>
""".format(core=CHIPS["core"], exp=CHIPS["exp"], lim=CHIPS["lim"])

# ----------------------------------------------------------- how it works ---

HOW = hero(
    kicker="How it works",
    title="A standard Bluetooth device — not a hack",
    lede="Elevon's core is boring in the best way: decades-old standards, used properly.",
) + """
<section class="wrap block">
  <ol class="steps">
    <li><strong>Your phone registers as an HID device</strong>Android 9+ ships the Bluetooth HID Device profile — the same standard a wireless keyboard uses. Elevon registers one composite descriptor: keyboard, mouse, media keys and gamepad together.</li>
    <li><strong>Your computer pairs once</strong>From the computer's Bluetooth settings, the phone looks like a proper input device. The host caches the descriptor at pairing time, which is exactly why one composite descriptor matters: you never re-pair when you switch modes.</li>
    <li><strong>Controls become reports</strong>Touching a keycap or stick turns into HID input reports on the interrupt channel — the same low-latency path a real mouse uses. Reports are throttled and warmed up after idle to beat Bluetooth power saving.</li>
    <li><strong>Switch modes any time</strong>Keyboard to gamepad to deck: same pairing, same connection. The computer just sees its input device doing different things.</li>
  </ol>
</section>
<section class="wrap block">
  <h2>Why no desktop app is needed — or allowed</h2>
  <p>Remote-control products (Unified Remote, Remote Mouse, Monect, KDE Connect) put a server on your
  computer because it gives them power: screen capture, system-wide keys, app launching. It also gives
  you setup friction, firewall prompts, background processes and security surface.</p>
  <p>Elevon takes the opposite trade for its core: standard HID reports, which every OS already
  understands. The rule is written into the project: <strong>the computer must not need any software for
  the core experience.</strong> Features that can't meet it are labeled Experimental or Limited instead
  of sneaking an installer in.</p>
  <div class="note"><strong>The full boundary</strong> — what's Core, what's Experimental, and what's honestly
  impossible without host software is catalogued in <a href="compatibility.html">Compatibility</a>.</div>
</section>
<section class="wrap block">
  <h2>Security &amp; privacy model</h2>
  <div class="grid three">
    <div class="card"><h3>Pairing is the gate</h3><p>Only computers you've paired with can connect, enforced by standard Bluetooth bonding. Unpair and the door is closed.</p></div>
    <div class="card"><h3>The app can't phone home</h3><p>No INTERNET permission. The OS blocks any network access — not a policy, a capability.</p></div>
    <div class="card"><h3>Relay is end-to-end encrypted</h3><p>The experimental laptop→phone direction uses ECDH P-256 and AES-GCM with a 5-digit comparison code, inside your browser.</p></div>
  </div>
</section>
"""

# ------------------------------------------------------- getting started ---

GETTING_STARTED = hero(
    kicker="Getting started",
    title="Pairing takes a minute. Then it's yours.",
    lede="No installers, no accounts, no restarts. If you've ever paired a Bluetooth keyboard, you already know how to do this.",
    actions=cta("Download from Releases", f"{REPO}releases", True),
) + """
<section class="wrap block">
  <div class="grid two">
    <div class="card">
      <h3>1 · Install Elevon on your phone</h3>
      <p>Grab the latest APK from GitHub Releases (signed, with SHA-256 checksums published). Android 9 or
      newer. Or build it yourself from source — it's a standard Android Studio project.</p>
    </div>
    <div class="card">
      <h3>2 · Open Elevon, allow Bluetooth</h3>
      <p>Elevon asks for Nearby-devices permission to act as a Bluetooth input device — never location.
      A compatibility check runs immediately and tells you plainly if your phone can't do it.</p>
    </div>
    <div class="card">
      <h3>3 · Pair from your computer</h3>
      <p>On the computer: Bluetooth settings → add device → pick your phone. Accept the prompt on the phone.
      That's the whole setup — the computer now believes it has a new keyboard, mouse and gamepad.</p>
    </div>
    <div class="card">
      <h3>4 · Connect and choose a control</h3>
      <p>Back in Elevon, your computer appears in the Connect panel. Tap Connect, pick Touchpad, and move
      your cursor. Switch to Keyboard, Gamepad, or a deck whenever you like — no re-pairing.</p>
    </div>
  </div>
  <div class="note"><strong>Reconnecting later:</strong> Elevon remembers your computers with friendly
  names. Open the app, tap Connect — or bind a profile so the right controls open automatically.</div>
</section>
<section class="wrap block">
  <h2>Downloading the APK safely</h2>
  <p>Releases are built publicly by CI. Each release page lists the APK's SHA-256 checksum; verify it with
  <code>sha256sum elevon-0.1.0.apk</code> before installing. Android will warn about unknown sources —
  that's normal for APKs outside the Play Store. Elevon is also trivially auditable: the entire app is a
  few thousand lines of Kotlin using only public APIs.</p>
  <div class="note warn"><strong>Heads-up:</strong> some Android versions flag any sideloaded app with
  Bluetooth permissions. That's the permission, not a detection. Check the signature and checksums, and
  read the code if you want certainty.</div>
</section>
"""

# ------------------------------------------------------------- controller ---

CONTROLLER = hero(
    kicker="Game controller",
    title="A controller that's honest about what it is",
    lede="Elevon's gamepad is a genuine generic HID controller. Here's exactly what that means for your games — no overclaiming.",
    actions=cta("Compatibility details", "compatibility.html"),
) + """
<section class="wrap block">
  <div class="feature">
    <div>
      <h2>Two output modes</h2>
      <div class="tag-row">{core}</div>
      <p><strong>Gamepad mode</strong> sends real HID gamepad reports: 16 buttons, two sticks,
      an eight-way hat, analog triggers. Steam, SDL-based games, emulators and controller-aware titles
      pick it up like any no-name controller.</p>
      <p><strong>Keyboard &amp; mouse mode</strong> maps the same on-screen controls to keys and mouse
      movement. This is the honest answer for games with no controller support at all — Minecraft Java
      included — and it works in <em>every</em> game, because every game supports keyboards.</p>
    </div>
    <div class="shot"><img src="assets/shot-gamepad.png" alt="Gamepad mode with editable control layout" loading="lazy"></div>
  </div>
</section>
<section class="wrap block">
  <h2>Profiles for the way you play</h2>
  <p>Every profile stores its layout, dead zones, sensitivity and output mode. Editing is direct:
  flip the edit toggle, drag controls where your thumbs want them, resize with a slider. Presets cover
  common setups — and each preset says which output mode it uses and why:</p>
  <div class="table-scroll">
  <table>
    <tr><th>Preset</th><th>Output</th><th>Why</th></tr>
    <tr><td>Standard</td><td class="yes">Gamepad</td><td>Controller-aware games, Steam titles, emulators</td></tr>
    <tr><td>GTA V / action</td><td class="yes">Gamepad</td><td>Native controller support; use Steam Input for XInput-only builds</td></tr>
    <tr><td>Forza / racing</td><td class="yes">Gamepad</td><td>Lower dead zone, higher sensitivity for steering</td></tr>
    <tr><td>Minecraft (keyboard)</td><td class="part">Keyboard &amp; mouse</td><td>Java Edition has no controller support — keys and mouse-look solve it for real</td></tr>
  </table>
  </div>
</section>
<section class="wrap block">
  <h2>The limits, plainly</h2>
  <div class="grid two">
    <div class="card"><h3>Not an Xbox controller {lim}</h3><p>Android doesn't let apps choose the Bluetooth vendor/product ID, so XInput impersonation is impossible. XInput-only games need Steam Input or in-game remapping. This is a platform wall, not a roadmap item.</p></div>
    <div class="card"><h3>Console support: no {lim}</h3><p>Game consoles authenticate controllers cryptographically. A phone cannot present itself as one. Elevon doesn't support consoles and doesn't promise to.</p></div>
    <div class="card"><h3>macOS gamepads {lim}</h3><p>Apple's GameController framework filters to known controllers, so a generic HID pad is invisible to it. Steam, emulators, SDL games and browsers see it fine.</p></div>
    <div class="card"><h3>Phone support varies {lim}</h3><p>The HID Device profile needs the phone maker to have enabled it. Most have. Elevon detects and tells you before you waste a minute.</p></div>
  </div>
</section>
""".format(core=CHIPS["core"], lim=CHIPS["lim"])

# --------------------------------------------------------------- keyboard ---

KEYBOARD = hero(
    kicker="Keyboard",
    title="A keyboard that controls, not a keyboard that texts",
    lede="Full desktop rows, one-handed compact, a gaming cluster, or your own arrangement — all on the one pairing.",
) + """
<section class="wrap block">
  <div class="feature">
    <div>
      <h2>Four layouts</h2>
      <div class="tag-row">{core}</div>
      <p><strong>Compact</strong> puts the alphabet and essential keys in thumb reach.
      <strong>Full</strong> is the desktop rows: function keys, punctuation, nav cluster.
      <strong>Gaming</strong> clusters WASD, Shift, Space and the keys games actually use.
      <strong>Custom</strong> lets you place any key in any slot — saved on your phone.</p>
    </div>
    <div class="shot"><img src="assets/shot-keyboard.png" alt="Keyboard layouts" loading="lazy"></div>
  </div>
  <div class="grid three">
    <div class="card"><h3>Modifiers that behave {core}</h3><p>Tap Ctrl to make it sticky, tap a letter to send the combo, and it clears. Hold a modifier to lock it for a whole session of shortcuts. Caps Lock syncs with the host's LED state.</p></div>
    <div class="card"><h3>Keys games need {core}</h3><p>Esc, Tab, F1–F12, arrows, Home/End, Page Up/Down, Insert/Delete — with auto-repeat when held, like a real keyboard.</p></div>
    <div class="card"><h3>Layouts that land correctly {core}</h3><p>Set the host layout (US, UK, German, French) so typed text matches the computer's own keyboard. AltGr and dead-key accents are documented limits — <a href="compatibility.html">details</a>.</p></div>
  </div>
</section>
""".format(core=CHIPS["core"])

# ---------------------------------------------------------- customization ---

CUSTOMIZATION = hero(
    kicker="Customization",
    title="Make it yours — then make it automatic",
    lede="Layouts, sizes, decks, themes and haptics are settings. Binding them to computers and games is profiles.",
) + """
<section class="wrap block">
  <div class="grid three">
    <div class="card"><h3>Controls</h3><p>Pointer and scroll speed, natural scrolling, tap-to-click, drag lock, keyboard layout, function row, keep-screen-on.</p></div>
    <div class="card"><h3>Appearance</h3><p>Graphite (dark) or Paper (light), following your system by default. One calm accent, no neon.</p></div>
    <div class="card"><h3>Haptics</h3><p>Off, subtle, or full — on keycaps, mode switches and slide advances.</p></div>
    <div class="card"><h3>Gamepad editor</h3><p>Drag any control, resize it, tune dead zones and sensitivity, save as a profile.</p></div>
    <div class="card"><h3>Custom keyboard</h3><p>Fill every slot from a palette of letters, digits, symbols, F-keys and arrows.</p></div>
    <div class="card"><h3>Deck editor</h3><p>Twelve slots per page; shortcuts, media keys, text snippets and waits; as many pages as you need.</p></div>
  </div>
</section>
<section class="wrap block">
  <h2>Profiles tie it together</h2>
  <ol class="steps">
    <li><strong>A profile remembers your setup</strong>Mode, host OS (for Cmd vs Ctrl), keyboard layout, gamepad profile, deck page.</li>
    <li><strong>Bind it to a computer</strong>“My Gaming PC” applies the gamepad profile the moment it connects. “Office Laptop” opens the touchpad with Mac shortcuts.</li>
    <li><strong>Switch any time</strong>One tap on Home, or long-press the app icon for the last device.</li>
  </ol>
</section>
"""

# ------------------------------------------------------------------ labs ---

LABS = hero(
    kicker="Elevon Labs",
    title="Experiments, clearly labeled",
    lede="Labs is where Elevon tries things that don't belong in a stable product yet — and says so.",
) + f"""
<section class="wrap block">
  <div class="card" style="border-color: var(--warning)">
    <h3>Relay {CHIPS['exp']}</h3>
    <p><strong>Use the keyboard already in front of you to type on your phone.</strong> Your laptop's browser
    connects to Elevon over Bluetooth, end-to-end encrypted, with a 5-digit code you compare. Nothing is
    installed on the laptop — the browser is the host software.</p>
    <p>Relay is honest about its boundaries: it sees keys only while its page is focused, it pauses when
    you switch apps, and it can never capture your system-wide keyboard. That's the browser's security
    model working as intended.</p>
    <div class="actions">{cta('Open Relay', 'relay/index.html', True)}</div>
  </div>
</section>
<section class="wrap block tight">
  <h2>Planned, not built</h2>
  <div class="grid three">
    <div class="card"><h3>Wi-Fi Relay {CHIPS['lim']}</h3><p>Would work in any browser, but needs the phone to serve a page — which means the INTERNET permission and losing the secure-context crypto. Re-evaluating; nothing shipped.</p></div>
    <div class="card"><h3>Deck sync {CHIPS['lim']}</h3><p>Encrypted opt-in sync or file export for decks across phones. Not started.</p></div>
    <div class="card"><h3>Pointer curves {CHIPS['lim']}</h3><p>Fine-grained acceleration tuning in plain HID reports. Exploring.</p></div>
  </div>
  <div class="note">Labs features can change or disappear between releases. Core features won't.</div>
</section>
"""

# ------------------------------------------------------------------- faq ---

FAQ = hero(
    kicker="FAQ",
    title="Questions, answered straight",
    lede="Including the ones other apps dodge.",
) + """
<section class="wrap block">
  <details open><summary>Does my computer need a companion application?</summary>
  <p>No. The core experience — keyboard, touchpad, gamepad, macro decks, media and presentation remotes —
  works over standard Bluetooth HID with nothing installed on the computer. This is a product principle,
  not a marketing line. The only browser-based extra (Relay) also installs nothing.</p></details>
  <details><summary>Can I use the phone as a keyboard?</summary>
  <p>Yes — that's the first mode. Full layouts with modifiers, function keys, navigation and auto-repeat,
  plus host-layout support for non-US computers.</p></details>
  <details><summary>Can I use it as a mouse?</summary>
  <p>Yes. The touchpad does cursor movement, tap-to-click, two-finger scroll (vertical and horizontal),
  right-click and press-and-hold drag, with explicit buttons too.</p></details>
  <details><summary>Can I use it as a game controller?</summary>
  <p>Yes, as a genuine generic HID gamepad — 16 buttons, two sticks, eight-way hat, analog triggers.
  Steam, emulators and controller-aware games work. XInput-only games need Steam Input because Android
  can't impersonate an Xbox pad. <a href="controller.html">Details →</a></p></details>
  <details><summary>Can I use it without internet?</summary>
  <p>Yes — entirely. Bluetooth only. The app has no INTERNET permission at all, so it cannot use the
  network even if it wanted to.</p></details>
  <details><summary>Can I use it with multiple computers?</summary>
  <p>Yes. Pair each computer once; they appear in Devices with friendly names, status, last-used time and
  an optional preferred profile that applies on connect.</p></details>
  <details><summary>Can I create game profiles?</summary>
  <p>Yes — layout, dead zones, sensitivity and output mode per game, with presets for common setups.
  Edit by dragging controls directly on the gamepad screen.</p></details>
  <details><summary>Does it work on Windows?</summary>
  <p>Windows 10 and 11: keyboard, mouse, touchpad, media keys and decks all work. Gamepad works in
  DirectInput/Raw Input titles and via Steam Input; XInput-only games are the documented exception.</p></details>
  <details><summary>Does it work on macOS?</summary>
  <p>Yes for keyboard, mouse, touchpad, media keys and decks. Shortcuts adapt automatically (Cmd instead
  of Ctrl). Gamepad works in generic-HID apps; Apple's GameController framework ignores generic pads.</p></details>
  <details><summary>Does it work on Linux?</summary>
  <p>Yes, including gamepads — Linux exposes generic HID devices to evdev/joydev and Steam/SDL pick them
  up happily. SteamOS too.</p></details>
  <details><summary>What is Elevon Relay?</summary>
  <p>An experimental Labs feature that reverses the direction: your laptop's physical keyboard types into
  your phone, through a browser page, over end-to-end-encrypted Bluetooth. Nothing is installed on the
  laptop. <a href="relay/index.html">Details →</a></p></details>
  <details><summary>Can Relay control the entire computer keyboard?</summary>
  <p>No — and note the direction: Relay sends keys <em>to the phone</em>. A web page only receives keys
  typed while its tab is focused; capturing a system-wide keyboard from a browser is impossible by
  design. Elevon states this rather than implying otherwise.</p></details>
  <details><summary>Why is Relay experimental?</summary>
  <p>It depends on Web Bluetooth (Chrome/Edge on Windows, macOS, ChromeOS), on a pairing flow that's
  still being polished, and on browser behaviors that can change. It works well for its target jobs —
  long messages, URLs, searches — but it's clearly labeled while it matures.</p></details>
  <details><summary>What happens if my phone doesn't support a capability?</summary>
  <p>Elevon tells you upfront, in plain language, and disables the affected parts instead of showing
  buttons that do nothing. The most common gap is phone makers shipping Bluetooth HID disabled.</p></details>
  <details><summary>Is my data uploaded anywhere?</summary>
  <p>No. The app has no INTERNET permission — the OS prevents any network use. Devices, profiles,
  layouts and clipboard history stay on the phone. Relay sessions are device-to-device and encrypted.</p></details>
</section>
"""

# ---------------------------------------------------------- compatibility ---

COMPAT = hero(
    kicker="Compatibility",
    title="What works where — the whole table",
    lede="No universal promises. If a cell says Limited, the reason is one click away.",
) + """
<section class="wrap block">
  <h2>Your phone</h2>
  <div class="table-scroll"><table>
    <tr><th>Requirement</th><th>Status</th></tr>
    <tr><td>Android 9.0 (2018) or newer</td><td class="yes">Required</td></tr>
    <tr><td>Bluetooth HID Device profile enabled by the manufacturer</td><td class="part">Almost always; a few OEMs disable it — Elevon detects this at first run</td></tr>
    <tr><td>Bluetooth LE advertising (Relay only)</td><td class="yes">Standard since this era</td></tr>
    <tr><td>Root access</td><td class="no">Never needed</td></tr>
  </table></div>
</section>
<section class="wrap block tight">
  <h2>Your computer</h2>
  <div class="table-scroll"><table>
    <tr><th>Host</th><th>Keyboard</th><th>Mouse / touchpad</th><th>Media keys</th><th>Gamepad</th><th>Decks</th></tr>
    <tr><td>Windows 10 / 11</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="part">DirectInput / Raw Input / Steam Input; XInput-only games need Steam</td><td class="yes">Yes</td></tr>
    <tr><td>macOS</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="part">Generic-HID apps only (Steam, emulators, browsers); not GameController framework</td><td class="yes">Yes</td></tr>
    <tr><td>Linux / SteamOS</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="yes">Yes</td></tr>
    <tr><td>ChromeOS</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="part">App-dependent</td><td class="yes">Yes</td></tr>
    <tr><td>iPadOS / iOS</td><td class="yes">Keyboard yes; pointer on iPadOS 13.4+</td><td class="part">Same note</td><td class="part">Partial</td><td class="no">No</td><td class="yes">Yes (as keys)</td></tr>
    <tr><td>Android TV / Google TV</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="yes">Yes</td><td class="yes">Mostly</td><td class="yes">Yes</td></tr>
    <tr><td>Game consoles</td><td class="no">No</td><td class="no">No</td><td class="no">No</td><td class="no">No</td><td class="no">No</td></tr>
  </table></div>
</section>
<section class="wrap block tight">
  <h2>Feature labels</h2>
  <p><span class="chip core">Core</span> works with nothing installed on the computer, on every supported host.
  <span class="chip experimental">Experimental</span> lives in Labs, installs nothing, and may change.
  <span class="chip limited">Limited</span> marks the specific boundaries explained above — stated, not hidden.</p>
  <h3>Documented technical limits</h3>
  <ul>
    <li><strong>XInput impersonation:</strong> impossible — apps can't set Bluetooth vendor/product IDs.</li>
    <li><strong>Boot-protocol devices:</strong> BIOS-level control isn't guaranteed; some pre-boot environments only speak the boot protocol.</li>
    <li><strong>Typed characters:</strong> sent as HID keystrokes on the host's layout. US/UK complete; German/French base keys mapped, AltGr/dead-key accents limited. (Relay text commits as text and is not affected.)</li>
    <li><strong>Wired USB HID:</strong> impossible without root on stock Android; not offered.</li>
    <li><strong>Relay browsers:</strong> Chrome/Edge on Windows, macOS, ChromeOS; Chrome on Linux needs a flag; Firefox/Safari don't implement Web Bluetooth.</li>
  </ul>
</section>
"""

# -------------------------------------------------------- troubleshooting ---

TROUBLESHOOTING = hero(
    kicker="Troubleshooting",
    title="When something doesn't work",
    lede="Plain steps, ordered by how often they fix the problem.",
) + """
<section class="wrap block">
  <h2>Connection</h2>
  <details open><summary>The computer doesn't see the phone, or pairing fails</summary>
  <p>1 · Open Elevon on the phone and leave it in the foreground. 2 · On the computer, remove/forget any
  old entry for this phone (both sides must be clean — half-removed pairings are the #1 cause).
  3 · Add a new device from the computer's Bluetooth settings and accept the prompt on the phone.
  4 · If the phone appears but says "not supported", your phone maker has disabled the HID profile —
  Elevon will have told you at first run.</p></details>
  <details><summary>Connected, but inputs do nothing</summary>
  <p>Check Elevon shows <strong>Connected</strong> (green dot) on Home. Then: some computers connect the
  phone's <em>audio</em> profiles too and get confused — remove the phone from the computer's sound
  settings, keep it in output devices. Finally, toggle airplane mode off/on on the phone and reconnect.</p></details>
  <details><summary>It keeps disconnecting</summary>
  <p>Bluetooth power saving on the phone can drop idle links. Keep Elevon's screen on while using it
  (automatic in control modes). Distance and 2.4 GHz Wi-Fi interference matter — try a metre away.</p></details>
  <details><summary>The first input after a pause is late</summary>
  <p>That's Bluetooth sniff mode (low-power idle). Elevon warms the link when you touch a control, so
  this should be barely noticeable; if it is, send any tap first.</p></details>
</section>
<section class="wrap block tight">
  <h2>Input feel</h2>
  <details><summary>Cursor too fast / too slow</summary>
  <p>Touchpad → tuning icon (sliders) → pointer speed. It multiplies movement per pixel of finger travel.</p></details>
  <details><summary>Scrolling goes the wrong way</summary>
  <p>Touchpad → tuning → natural scrolling. Off mimics a Windows touchpad; on mimics a phone.</p></details>
  <details><summary>Keys type wrong characters</summary>
  <p>Set Settings → Keyboard → host layout to match the computer's configured layout. US/UK map fully;
  German/French map the base keys with AltGr/dead-key accents documented as limited.</p></details>
  <details><summary>Gamepad buttons are mapped oddly in a game</summary>
  <p>Generic HID pads can land with a different button order per game engine. Remap in-game once, or use
  Steam Input, which normalises everything. If a D-pad direction is simply missing, that's a bug —
  <a href="https://github.com/MeYashverma/NEXUS/issues">file it</a>.</p></details>
</section>
<section class="wrap block tight">
  <h2>Relay</h2>
  <details><summary>The laptop can't find the phone</summary>
  <p>Use Chrome or Edge (Relay needs Web Bluetooth). Start Relay on the phone first, keep its screen on,
  then open elevon.app/relay and click Connect. On Linux, Chrome needs
  <code>chrome://flags/#enable-experimental-web-platform-features</code> enabled. Firefox and Safari
  don't support Web Bluetooth at all.</p></details>
  <details><summary>Typed keys don't arrive on the phone</summary>
  <p>Relay types into the focused field only. Make sure the Relay page is the focused tab, the phone shows
  RELAY ACTIVE, and — if you want text in other apps — the Elevon Relay keyboard is enabled and selected.
  Switching apps on the laptop pauses Relay deliberately.</p></details>
  <details><summary>The codes don't match</summary>
  <p>Stop immediately and start again — a mismatch means something is between you and your phone.
  Never accept a code you didn't verify.</p></details>
</section>
<section class="wrap block tight">
  <h2>Still stuck?</h2>
  <p><a href="https://github.com/MeYashverma/NEXUS/issues">Open an issue</a> with: phone model + Android
  version, computer OS, Elevon version, and what you saw. Screenshots of the connection state help a lot.</p>
</section>
"""

# ---------------------------------------------------------------- privacy ---

PRIVACY = hero(
    kicker="Privacy",
    title="What Elevon knows: nothing it doesn't need on your phone",
    lede="No account. No analytics. No INTERNET permission — your OS blocks the network entirely.",
) + """
<section class="wrap block">
  <div class="grid two">
    <div class="card"><h3>What stays on the phone</h3>
      <p>Paired computers and their names · profiles · gamepad layouts · macro decks · keyboard layouts ·
      clipboard history (if enabled) · Relay session state. All in the app's private storage. Uninstall
      and it's gone.</p></div>
    <div class="card"><h3>What crosses the air</h3>
      <p>Bluetooth HID input reports (keystrokes, mouse deltas, gamepad state) to computers you paired with,
      protected by standard Bluetooth bonding. That's the product. Nothing else, to anyone, ever.</p></div>
    <div class="card"><h3>Clipboard</h3>
      <p>Text you send (typed to the computer or received via Relay) can be remembered locally — the last
      20 items, shown in the Clipboard page. Retention can be switched off entirely, and one button wipes
      the history. Elevon never reads your clipboard in the background; nothing runs unless you're using it.</p></div>
    <div class="card"><h3>Relay specifically</h3>
      <p>Typed text is end-to-end encrypted (ECDH P-256 + AES-GCM) between your browser and the phone; the
      5-digit code exists so you can verify there's no man in the middle. The Relay page is static — it has
      no server, no logging, no analytics. When you press STOP, the buffer is cleared.</p></div>
    <div class="card"><h3>Diagnostics</h3>
      <p>None. No crash reporting, no usage stats, no flags. Bug reports are manual and contain only what
      you choose to paste into a GitHub issue.</p></div>
    <div class="card"><h3>Deleting your data</h3>
      <p>Devices → forget. Clipboard → delete all history. Everything else: uninstall the app. Because
      there's no server, there's nothing to request removal of.</p></div>
  </div>
  <div class="note"><strong>Why we can promise this:</strong> the permission list in the Android manifest is
  short and auditable — Bluetooth roles, foreground service, notifications, vibration. There is no
  INTERNET permission, so data exfiltration isn't a policy question; it's impossible. Read the manifest
  yourself in the repo.</div>
</section>
"""

# --------------------------------------------------------------- changelog ---

CHANGELOG = hero(
    kicker="Changelog",
    title="Releases",
    lede="Versioning: semver. Every release ships with signed APKs, SHA-256 checksums and notes.",
    actions=cta("All releases", f"{REPO}releases", True),
) + f"""
<section class="wrap block">
  <div class="card">
    <h3>v{VERSION} — first public alpha <span class="chip experimental">Alpha</span></h3>
    <p><em>Released: September 2026</em></p>
    <h4>Added</h4>
    <ul>
      <li>Bluetooth HID core: composite keyboard + mouse + media + gamepad descriptor; single pairing for all modes</li>
      <li>Keyboard mode: compact, full, gaming and custom layouts; sticky/locked modifiers; auto-repeat; host layout setting</li>
      <li>Touchpad mode: tap-to-click, two-finger scroll (vertical + horizontal), press-and-hold drag with drag lock, tuning sliders</li>
      <li>Gamepad mode: editable layouts, dead zones, sensitivity, hat-switch D-pad, analog triggers, stick clicks; keyboard &amp; mouse output mode</li>
      <li>Macro pad &amp; custom decks: 12-slot pages, OS-aware shortcut presets, media keys, text snippets, waits</li>
      <li>Media and presentation remotes (arrow/page/N-P advance, timer, blank)</li>
      <li>Profiles bound to devices with per-computer OS; friendly device names</li>
      <li>Elevon Labs with Relay (browser ↔ phone over BLE, ECDH P-256 + AES-GCM, comparison code, RELAY ACTIVE banner, STOP)</li>
      <li>Clipboard bridge (typed to computer; history with retention off-switch)</li>
    </ul>
    <h4>Known issues</h4>
    <ul>
      <li>XInput-only games don't see the gamepad without Steam Input (platform limitation, documented)</li>
      <li>Relay requires Chromium-based browsers; Linux needs a Chrome flag</li>
      <li>Some OEMs ship with the Bluetooth HID profile disabled — the app detects and explains, but can't fix firmware</li>
    </ul>
    <h4>Upgrade notes</h4>
    <p>First release. If you paired a test build with a different descriptor, unpair on both sides once.</p>
  </div>
</section>
"""

# ---------------------------------------------------------------- roadmap ---

ROADMAP = hero(
    kicker="Roadmap",
    title="Where Elevon is going",
    lede="Completed means shipped. Planned means scheduled. Experimental means Labs. Nothing here is decorative.",
) + f"""
<section class="wrap block">
  <div class="grid two">
    <div class="card">
      <h3>Completed <span class="chip core">v{VERSION}</span></h3>
      <ul>
        <li>All seven control modes over one pairing</li>
        <li>Profiles, devices, per-computer OS awareness</li>
        <li>Macro decks and custom surfaces without host software</li>
        <li>Elevon Labs + Relay (browser, encrypted)</li>
        <li>Honest compatibility detection and labeling</li>
      </ul>
    </div>
    <div class="card">
      <h3>In progress</h3>
      <ul>
        <li>Relay polish: reconnect flow, IME switching UX</li>
        <li>Game profile presets, tested per title (help wanted — <a href="contributing.html">see Contributing</a>)</li>
        <li>Accessibility audit with TalkBack users</li>
      </ul>
    </div>
    <div class="card">
      <h3>Planned</h3>
      <ul>
        <li>Numpad mode</li>
        <li>Deck export/import (file-based, no cloud)</li>
        <li>F-Droid distribution</li>
        <li>More host layouts for typed text</li>
        <li>Pointer acceleration curves</li>
      </ul>
    </div>
    <div class="card">
      <h3>Experimental (Labs)</h3>
      <ul>
        <li>Relay — current focus</li>
        <li>Wi-Fi Relay variant (blocked on the INTERNET-permission trade-off, documented in Labs)</li>
        <li>Deck sync (undecided shape)</li>
      </ul>
    </div>
  </div>
  <div class="note">Deliberately <strong>not</strong> on the roadmap: screen streaming, file transfer, console
  support, XInput impersonation. Each is either impossible without host software or without platform
  capabilities Android doesn't expose — the research explains why in the repo's docs.</div>
</section>
"""

# ------------------------------------------------------------ contributing ---

CONTRIBUTING = hero(
    kicker="Contributing",
    title="Help build the missing input device",
    lede="Code, docs, game profiles, translations, bug reports with real devices — all welcome.",
) + f"""
<section class="wrap block">
  <ol class="steps">
    <li><strong>Find something to do</strong>Check <a href="{REPO}/issues">issues</a> and the roadmap. Device-specific test reports are as valuable as code.</li>
    <li><strong>Set up the project</strong>Clone the repo, open <code>app/</code> in Android Studio (or run <code>./gradlew assembleDebug</code> — CI proves it builds clean).</li>
    <li><strong>Follow the product rules</strong>No host software for core features. No new permissions without a written justification. No fake features. Honest labels everywhere.</li>
    <li><strong>Open a pull request</strong>Small PRs land faster. Unit tests cover HID reports, descriptor parsing, crypto and JSON models — extend them when you touch those areas.</li>
  </ol>
  <div class="grid two">
    <div class="card"><h3>Especially wanted</h3>
      <ul><li>Real-device testing on phones with/without the HID profile</li>
      <li>Game-profile presets with verified mappings</li>
      <li>Host-layout tables beyond US/UK/DE/FR</li>
      <li>Accessibility review (TalkBack, switch access)</li></ul></div>
    <div class="card"><h3>Ground rules</h3>
      <ul><li>Apache-2.0, contributor-friendly</li>
      <li><a href="{REPO}/blob/main/CODE_OF_CONDUCT.md">Code of conduct</a> applies everywhere</li>
      <li>No telemetry, ever — patches that add analytics will be declined</li>
      <li>Security issues: see SECURITY.md, please don't open public issues</li></ul></div>
  </div>
</section>
"""

# ------------------------------------------------------------------ about ---

ABOUT = hero(
    kicker="About",
    title="One surface, many jobs",
    lede="An elevon is the part of an aircraft that does the work of two control surfaces at once. That's the whole product idea.",
) + f"""
<section class="wrap block">
  <p>Elevon started from a simple observation: every "phone as computer remote" product either demands a
  desktop server, or does exactly one thing well and charges for the rest. Meanwhile Android has shipped
  the ability to be a <em>real</em> Bluetooth input device since 2018 — barely any app uses it for more
  than a keyboard and mouse.</p>
  <p>This project is the attempt to do it properly: every mode, one pairing, nothing to install on the
  computer, free and open source, with limits stated on the box. The research behind these decisions —
  including the competitive analysis and the reason the working name NEXUS was replaced — is published in
  the repository.</p>
  <div class="grid three">
    <div class="card"><h3>Principles</h3><ul>
      <li>The computer needs nothing installed for the core</li>
      <li>Honesty about limits is a feature</li>
      <li>Privacy by capability, not policy</li>
      <li>An instrument, not a utility</li></ul></div>
    <div class="card"><h3>Links</h3><ul>
      <li><a href="{REPO}">GitHub repository</a></li>
      <li><a href="{REPO}/releases">Releases</a></li>
      <li><a href="{REPO}/issues">Issues</a></li>
      <li><a href="{REPO}/discussions">Discussions</a></li></ul></div>
    <div class="card"><h3>License</h3><p>Apache-2.0 for code. Brand assets in <code>branding/</code> are
    part of the project; the name Elevon is used by this project — do a proper trademark search before
    commercial reuse.</p></div>
  </div>
</section>
"""

# ------------------------------------------------------------------ build ---

build_page("index.html", "Your phone. Your controls.", "Elevon turns your Android phone into a wireless keyboard, touchpad, gamepad, macro deck and remote for any computer — with nothing to install on the computer.", INDEX)
build_page("features.html", "Features", "Keyboard, touchpad, gamepad, macro pad, media and presentation remotes, profiles and Relay — every Elevon control mode explained.", FEATURES)
build_page("how-it-works.html", "How it works", "Why Elevon needs no desktop app: standard Bluetooth HID, one composite pairing, honest limits.", HOW)
build_page("getting-started.html", "Getting started", "Install Elevon, pair your computer over Bluetooth, and start controlling — a two-minute setup with no host software.", GETTING_STARTED)
build_page("keyboard.html", "Keyboard mode", "Compact, full, gaming and custom keyboard layouts with real modifiers, function keys and host layout support.", KEYBOARD)
build_page("controller.html", "Game controller", "Elevon's gamepad: editable layouts, dead zones, profiles, and two honest output modes — gamepad or keyboard & mouse.", CONTROLLER)
build_page("customization.html", "Customization", "Layouts, decks, themes, haptics — and profiles that bind your setup to each computer automatically.", CUSTOMIZATION)
build_page("labs.html", "Elevon Labs", "Experimental Elevon features: Relay, the laptop-to-phone typing bridge, and what's planned next.", LABS)
build_page("compatibility.html", "Compatibility", "The full Elevon compatibility table: phones, computers, gamepads, browsers — with limits stated plainly.", COMPAT)
build_page("faq.html", "FAQ", "Does the computer need an app? Does it work on Windows, macOS, Linux? What is Relay? Straight answers.", FAQ)
build_page("troubleshooting.html", "Troubleshooting", "Fixes for pairing, laggy inputs, wrong characters, disconnects and Relay issues — in plain language.", TROUBLESHOOTING)
build_page("privacy.html", "Privacy", "No INTERNET permission, no accounts, no analytics. Exactly what Elevon stores, sends and deletes.", PRIVACY)
build_page("changelog.html", "Changelog", "Elevon release notes and version history.", CHANGELOG)
build_page("roadmap.html", "Roadmap", "What's completed, in progress, planned and experimental in Elevon.", ROADMAP)
build_page("contributing.html", "Contributing", "How to contribute to Elevon: code, docs, device testing, game profiles and ground rules.", CONTRIBUTING)
build_page("about.html", "About", "About Elevon — one surface, many jobs — and the principles behind the project.", ABOUT)
