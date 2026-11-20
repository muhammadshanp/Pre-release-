# pre re

**The pre-release workspace** — stage, preview and promote every release before the world sees it.

This repository holds the marketing site for *pre re*, built as a zero-dependency static site.

## Branding

| Asset | Path | Notes |
| --- | --- | --- |
| App logo (source) | `assets/logo.png` | 1024×1024 master |
| App logo (scalable) | `assets/logo.svg` | Rebuilt as vectors — same geometry as the master |
| Mark only | `assets/mark.svg` | White mark on transparent, inherits `currentColor` |
| Favicons | `assets/icons/` + `assets/favicon.ico` | 32 / 64 / 180 / 192 / 512 px |
| Web app manifest | `manifest.webmanifest` | Installs as **pre re** |

Logo geometry (derived from the master artwork): `#111827` tile with a 239.5px
corner radius on a 1024px canvas; white circle at (512, 512) r 239.5 with an
`#111827` cutout at (591.5, 511.5) r 112.

## Run locally

No build step, no dependencies.

```bash
python3 -m http.server 8000 --bind 0.0.0.0
# → http://localhost:8000
```

## Structure

```
index.html               # single-page site
manifest.webmanifest
assets/
  css/styles.css         # design tokens, layout, responsive rules
  js/main.js             # nav, scroll reveal, waitlist forms
  logo.svg  mark.svg  logo.png
  icons/                 # favicon + app icon set
```

## Notes

- The waitlist forms validate client-side and persist addresses to `localStorage`
  (`prere.waitlist`) — there is no backend yet. Point them at your API when one exists.
- Dark theme matches the logo tile; motion respects `prefers-reduced-motion`.
- Type is Inter, falling back to the system UI stack if the font CDN is unavailable.
