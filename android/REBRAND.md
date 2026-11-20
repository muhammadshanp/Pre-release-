# pre re — rebrand notes

This folder is the Android app (Kotlin + Jetpack Compose) rebranded from **P Vault**
to **pre re**, with the supplied logo. Source imported from the `p-vault-main` upload.

## What changed

| Area | Before | After |
| --- | --- | --- |
| Launcher label | `P Vault` / `P Vault Pro` | `pre re` / `pre re Pro` (`app/build.gradle.kts`, `resValue app_name`) |
| Launcher icon | generic foreground on `#16181D` | your ring mark on `#111827` |
| Icon densities | `xxxhdpi` only | `mdpi 108` → `xxxhdpi 432` (`drawable-*/ic_launcher_foreground.png`) |
| Adaptive-icon background | `#16181D` | `#111827` (`values/colors.xml`) |
| Gradle project name | `PVault` | `pre-re` (`settings.gradle.kts`) |
| User-visible text | `P Vault` | `pre re` (32 files: screens, notifications, dialogs, README, workflows) |
| `icon.png` | old artwork | your logo tile (1024×1024) |

The icon foreground was generated at the correct adaptive-icon proportions — a 108dp
canvas with the mark inside the 72dp safe zone — and verified against the analytic
area of the ring (0.2727 coverage, exact to 4 decimal places at every density).

## What was deliberately left alone — and why

These strings look like the brand but are **compatibility identifiers**. Renaming
them would break existing installs, backups, or server config:

| Kept | Where | Why |
| --- | --- | --- |
| `com.dudeapp.vault` | `applicationId`, `namespace`, package dirs | Changes app identity: nothing installs over an existing build, and Firebase/Google config no longer matches. |
| `p-vault-app` | Firebase project id (`AdminPanel.kt`, `Access.kt`, `FirebaseSession.kt`) | Server-side project id. Must match `google-services.json`. |
| `pvault` | `BackupCrypto.EXTENSION` | Backup file extension. Renaming makes old backups unreadable. |
| `PVAULT-ENC-1` | `BackupCrypto.MAGIC` | Header inside encrypted backups, checked on restore. |
| `P_Vault_*` | `DriveBackup.kt` | Backup discovery prefix; old backups would stop being found. |
| `P-Vault.apk` | `.github/workflows/release.yml` | Release asset name is baked into the in-app update URL. |
| `P-Vault-update.apk` | `AppUpdater.kt` | Local temp filename only. |
| `PVaultTheme` | Compose theme function | Internal symbol, never shown to users. |
| `pvault` | `Usernames.kt` reserved list | Reserved-name list; harmless. |

## Before you ship this — decide these

1. **The updater still points at the old product.**
   `UpdateChecker.kt` fetches
   `https://github.com/muhammadshanp/p-vault/releases/latest/download/P-Vault.apk`.
   Because the applicationId and signing key are unchanged, a rebranded **pre re**
   install would happily download and install an old **P Vault** build over itself.
   Point `UPDATE_REPO` / `APK_URL` at the repo you will actually publish from, and
   keep the asset name in `release.yml` and that URL in sync.

2. **Version name drives the label.** `versionName = "0.5.48-pro"` contains `-`, so
   the app shows as **pre re Pro**. Drop the dash (e.g. `0.5.48`) for **pre re**.

3. **Signing key.** Releases must be signed with the same key as previous builds or
   Android refuses to install the update. `release.yml` enforces this by fingerprint.

4. **Optional, for a clean break:** changing `applicationId` (e.g.
   `dev.prere.vault`) makes this a genuinely separate app — fresh Firebase project,
   new `google-services.json`, no update path from P Vault, no shared backups.
   Everything else in this table would then need renaming too.

## Build

Not buildable in this workspace (no Android SDK, and Gradle needs `google()` /
`mavenCentral()`, which are unreachable here) — **no APK was produced**. Build it
locally with Android Studio or:

```bash
cd android
./gradlew :app:assembleRelease    # needs ANDROID_HOME + JDK 17
./gradlew :app:test               # unit tests
```

`build.ps1` wraps the same for Windows.

## Known-good checks run here

- all `app/src/main/**/*.xml` parse cleanly
- `google-services.json` and `theme-catalog/themes.json` parse cleanly
- launcher icon PNGs: RGBA, correct alpha, correct geometry at all five densities
- no remaining user-visible `P Vault` strings (only the compatibility identifiers above)

Not run here: Kotlin compilation, Gradle build, unit tests, Espresso/Compose UI.
Those need a real Android toolchain.
