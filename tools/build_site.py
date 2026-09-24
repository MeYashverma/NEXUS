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
VERSION = "0.1.1"

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
        announce = f'<div class="announce">✦ v{VERSION} — HID & permission fixes shipped · <a href="{rel("changelog.html")}">See what\'s new</a> · <a href="{REPO}/releases/tag/v{VERSION}">Download APK</a></div>'
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
      <svg width="26" height="26" viewBox="0 0 108 108" aria-hidden="true"><path fill="#FF7A45" d="M35,38.5 C35,33.9 40,30.9 44,33 L70.5,46.4 C75,48.7 75,55.3 70.5,57.6 L44,71 C40,73.1 35,70.1 35,65.5 Z"/><path fill="#101114" d="M46,51.5 L63,51.5 A2.5,2.5 0 0 1 65.5,54 A2.5,2.5 0 0 1 63,56.5 L46,56.5 A2.5,2.5 0 0 1 43.5,54 A2.5,2.5 0 0 1 46,51.5 Z"/></svg>
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
      <svg width="22" height="22" viewBox="0 0 108 108" aria-hidden="true"><path fill="#FF7A45" d="M35,38.5 C35,33.9 40,30.9 44,33 L70.5,46.4 C75,48.7 75,55.3 70.5,57.6 L44,71 C40,73.1 35,70.1 35,65.5 Z"/></svg>
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
