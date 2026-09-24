#!/usr/bin/env python3
"""Builds the Elevon website into website/.

Why a generator: 17 pages share one header/footer/nav; hand-duplicating that
invites drift. The generated HTML is committed, so GitHub Pages can serve
website/ directly with no build step. Run from repo root:
    python3 tools/build_site.py
Page bodies live in tools/pages_source.py.
"""

import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "website"

REPO = "https://github.com/MeYashverma/NEXUS"
SITE_ROOT = "https://meyashverma.github.io/NEXUS"
VERSION = "0.2.0"

NAV = [
    ("index.html", "Home"),
    ("features.html", "Features"),
    ("relay/index.html", "Relay"),
    ("getting-started.html", "Get started"),
    ("faq.html", "FAQ"),
    ("about.html", "About"),
]

FOOTER_GROUPS = [
    (
        "Product",
        [
            ("index.html", "Home"),
            ("features.html", "Features"),
            ("how-it-works.html", "How it works"),
            ("getting-started.html", "Getting started"),
            ("relay/index.html", "Elevon Relay"),
        ],
    ),
    (
        "Controls",
        [
            ("keyboard.html", "Keyboard"),
            ("controller.html", "Game controller"),
            ("customization.html", "Customization"),
            ("labs.html", "Labs"),
        ],
    ),
    (
        "Support",
        [
            ("compatibility.html", "Compatibility"),
            ("faq.html", "FAQ"),
            ("troubleshooting.html", "Troubleshooting"),
            ("changelog.html", "Changelog"),
            ("roadmap.html", "Roadmap"),
        ],
    ),
    (
        "Project",
        [
            ("privacy.html", "Privacy"),
            ("contributing.html", "Contributing"),
            ("about.html", "About"),
            (f"{REPO}/releases", "Releases"),
            (REPO, "GitHub"),
        ],
    ),
]


def build_page(path: str, title: str, description: str, body: str) -> None:
    in_relay = path.startswith("relay/")
    prefix = "../" if in_relay else ""

    def rel(href: str) -> str:
        if href.startswith("http"):
            return href
        return prefix + href

    nav_html = "".join(
        f'<a href="{rel(href)}" class="navlink{" active" if path == href else ""}">{label}</a>'
        for href, label in NAV
    )
    groups = "".join(
        "<div class='footcol'><h4>{}</h4>{}</div>".format(
            gtitle,
            "".join(f'<a href="{rel(href)}">{label}</a>' for href, label in items),
        )
        for gtitle, items in FOOTER_GROUPS
    )
    announce = ""
    if path == "index.html":
        announce = f'<div class="announce">✦ v{VERSION} — E-wing logo, fullscreen gamepad & keyboard, numpad, gyro mouse, pointer curves · <a href="{rel("changelog.html")}">See what\'s new</a> · <a href="{REPO}/releases/tag/v{VERSION}">Download APK</a></div>'
    html = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title} · Elevon</title>
<meta name="description" content="{description}">
<meta property="og:title" content="{title} · Elevon">
<meta property="og:description" content="{description}">
<meta property="og:image" content="{SITE_ROOT}/assets/social-card.png">
<meta property="og:type" content="website">
<meta name="theme-color" content="#101114">
<link rel="icon" type="image/svg+xml" href="{prefix}assets/favicon.svg">
<link rel="stylesheet" href="{prefix}styles.css">
<link rel="preconnect" href="https://fonts.googleapis.com">
</head>
<body>
<a class="skip" href="#main">Skip to content</a>
{announce}
<header>
  <div class="wrap bar">
    <a class="brand" href="{rel('index.html')}" aria-label="Elevon home">
      <svg width="26" height="26" viewBox="0 0 108 108" aria-hidden="true"><rect width="108" height="108" rx="26" fill="#101114"/><path fill="#FF7A45" d="M20,92 L37,20 C38.2,15.5 41.5,12.5 46,11 L88,1.5 L76,20 L50,27 C47,27.8 44.8,29.5 44,32.5 L42,41.5 L60,33.5 L80,26.5 L68.5,47 L44.5,57 C41.5,58.2 39.5,60.2 38.5,63 L36.5,72 L74,72 L61.5,90 L26,90 C22.5,90 19.5,90.5 20,92 Z"/></svg>
      <span>Elevon</span>
    </a>
    <nav aria-label="Primary">{nav_html}</nav>
    <a class="cta-ghost" href="{REPO}" target="_blank" rel="noopener">GitHub</a>
  </div>
</header>
<main id="main">
{body}
</main>
<footer>
  <div class="wrap foot">
    <div class="foot-brand">
      <svg width="22" height="22" viewBox="0 0 108 108" aria-hidden="true"><rect width="108" height="108" rx="26" fill="#101114"/><path fill="#FF7A45" d="M20,92 L37,20 C38.2,15.5 41.5,12.5 46,11 L88,1.5 L76,20 L50,27 C47,27.8 44.8,29.5 44,32.5 L42,41.5 L60,33.5 L80,26.5 L68.5,47 L44.5,57 C41.5,58.2 39.5,60.2 38.5,63 L36.5,72 L74,72 L61.5,90 L26,90 C22.5,90 19.5,90.5 20,92 Z"/></svg>
      <div>
        <strong>Elevon</strong>
        <p>Your phone. Your controls.<br>Open source under Apache-2.0.<br>No accounts, no tracking, no internet permission.</p>
      </div>
    </div>
    {groups}
  </div>
  <div class="wrap legal">
    <span>© 2026 Elevon contributors · v{VERSION}</span>
    <span>An elevon is a control surface that does the job of two.</span>
  </div>
</footer>
</body>
</html>
"""
    target = OUT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(html, encoding="utf-8")
    print(f"wrote {target.relative_to(ROOT)}")


if __name__ == "__main__":
    import pages_source  # noqa: F401 — importing it builds every page

    print("done.")
