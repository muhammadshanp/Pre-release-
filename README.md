# pre re

**The pre-release workspace** — stage, preview and promote every release before the world sees it.

This repository holds two things:

| Path | What it is |
| --- | --- |
| `/` (`index.html`) | The **pre re** marketing site — a zero-dependency static page |
| `/android` | The **pre re** Android app (Kotlin + Jetpack Compose) |

## Branding

| Asset | Path | Notes |
| --- | --- | --- |
| App logo (source) | `assets/logo.png` | 1024×1024 master |
| App logo (scalable) | `assets/logo.svg` | Rebuilt as vectors — same geometry as the master |
| Mark only | `assets/mark.svg` | White mark on transparent, inherits `currentColor` |
| Web icons | `assets/icons/` + `assets/favicon.ico` | 32 / 64 / 180 / 192 / 512 px |
| Android launcher | `android/app/src/main/res/drawable-*/` | Adaptive foreground, 108dp canvas, 72dp safe zone |
| Web app manifest | `manifest.webmanifest` | Installs as **pre re** |

Logo geometry (derived from the master artwork): `#111827` tile with a 239.5px
corner radius on a 1024px canvas; white circle at (512, 512) r 239.5 with an
`#111827` cutout at (591.5, 511.5) r 112.

## Website

No build step, no dependencies:

```bash
python3 -m http.server 8000 --bind 0.0.0.0
# → http://localhost:8000
```

```
index.html               # single-page site
manifest.webmanifest
assets/
  css/styles.css         # design tokens, layout, responsive rules
  js/main.js             # nav, scroll reveal, waitlist forms
  logo.svg  mark.svg  logo.png
  icons/                 # favicon + app icon set
```

The waitlist forms validate client-side and persist to `localStorage`
(`prere.waitlist`) — there is no backend yet.

## Android app

Full native vault app: photos, PDFs, notes and reels, PIN + biometric lock, optional
Google Drive backup, themes, push notifications, in-app updater.

```bash
cd android
./gradlew :app:assembleRelease    # needs ANDROID_HOME + JDK 17
./gradlew :app:test
```

The app was rebranded from P Vault. **Read [`android/REBRAND.md`](android/REBRAND.md)
before shipping** — it lists what changed, which identifiers were deliberately kept
for compatibility (applicationId, Firebase project, backup format), and the updater
issue that still points at the old product's releases.

Note: no APK is built in this repo's CI for the app yet, and the uploaded
`p-vault-main.zip` at the repo root is the original source archive — safe to delete.
