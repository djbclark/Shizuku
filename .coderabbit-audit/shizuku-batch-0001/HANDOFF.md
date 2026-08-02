# HANDOFF — Shizuku fleet/headless fork

Last updated: **2026-07-31**
Current release: **v13.7.0-thedjchi+stayturgid-release25** published. Since release17 (below), releases 21–25 landed: native-lib packaging fix, BootRetryWorker/AdbStarter hardening (H3, now retries indefinitely instead of capping at 5 attempts), a signing-certificate trust allowlist for the silent-grant-loss bug (#4), CI release signing secrets (H1, #5), and idempotent tag/release creation in `app.yml` (#9).

## 2026-07-31 update

- PR #4 merged: signing-certificate trust allowlist in `ShizukuConfigManager.find(uid)` — works around `org.stayturgid.agent`'s Shizuku grant silently resetting (even from a plain `shizuku_server` restart, no reboot/reinstall). See `docs/trusted-signer-allowlist.md`.
- PR #5 merged: H1 (CI signing secrets) resolved — a dedicated release keystore is now configured as GitHub Actions secrets on this repo; CI release builds are properly signed instead of silently falling back to the shared Android debug keystore. The "Fork limitation" note further down about missing `KEYSTORE` secrets is now stale/resolved.
- PR #7 merged: `BootRetryWorker`'s 5-attempt cap was removed — it now retries indefinitely (WorkManager's own exponential backoff, capped ~5h/attempt), re-checking the start-on-boot setting on every attempt. The 2026-07-25 entry below describing "max 5 attempts" reflects that update's state at the time, not current behavior.
- PR #6 reconciled H3/M2 as resolved based on the state as of 2026-07-31 17:10 UTC; PR #7 (2026-07-31 22:18 UTC) changed H3's behavior again afterward (see above) — noting here so this doesn't go stale a second time.

## 2026-07-25 update

- PR #1 merged to `fork/master`: `useLegacyPackaging = false` in `manager/build.gradle` — fixes native libraries not surviving Fire OS 8 in-place upgrades (they were extracted-on-install rather than stored uncompressed in the APK, and Fire OS 8 skipped re-extraction on upgrade). Also fixes a Fire OS notification icon resource lookup.
- This was hotfixed as a manually side-loaded debug build directly on the hd8 fleet device ahead of a proper release cut, so hd8's installed `versionCode` briefly ran ahead of the last published GitHub release — the normal ansible `bootstrap_apks` pinned-release install path saw that as `INSTALL_FAILED_VERSION_DOWNGRADE` and skipped it.
- `BootRetryWorker` (H3) also improved: exponential backoff (10s base, max 5 attempts, ~2.5min window), each attempt re-verifies `isRunning()` after a 3s delay.

---

## Repository

- **Fork:** `djbclark/Shizuku` (fork of `thedjchi/Shizuku`, which is itself a fork of `RikkaApps/Shizuku`)
- **Base:** `v13.7.0-thedjchi` (upstream release tag)
- **Branch:** `master` (force-pushed regularly — single-developer workflow)
- **Purpose:** Add fleet/headless automation primitives for the [stayturgid](https://github.com/djbclark/stayturgid) project — a fleet of Android devices (Samsung S24, Pixel 7a, Fire HD8) running stayturgid's native Kotlin agent (`device/native-agent/`) + Shizuku. (Earlier in this fork's history the fleet ran AutoJs6 + Shizuku; AutoJs6 was retired fleet-wide in favor of the native agent.)

---

## Release naming convention

### APK filename

```
shizuku-v<versionName>-<abi>.apk
```

| Part | Source | Example |
|---|---|---|
| **versionName** | `rootProject.ext.versionName` in `build.gradle:37` | `13.7.0-thedjchi+stayturgid-release11` |
| **abi** | Build ABI filter, defaults to `universal` | `universal` |

**Example:** `shizuku-v13.7.0-thedjchi+stayturgid-release11-universal.apk`

### VersionName format

Computed in `build.gradle:35-38`:

```
13.7.0[-beta]-thedjchi+stayturgid-release<N>
```

| Part | Meaning | Example |
|---|---|---|
| `13.7.0` | Upstream base version | `13.7.0` |
| `-beta` | Optional — only when `-Pbeta` passed to Gradle | `-beta` |
| `-thedjchi` | Fork identifier (inherited from thedjchi) | `-thedjchi` |
| `+stayturgid-release<N>` | Build metadata (`+` separator), `N` = commits since upstream tag | `+stayturgid-release11` |

The `+` is semver build metadata separator — version comparison ignores it, but the string is unique per release. Obtainium matches installed APK `versionName` against GitHub release tags (ignoring the `v` prefix).

`N` comes from `git rev-list --count v13.7.0-thedjchi..HEAD` — auto-increments with each commit.

### VersionCode

```
50000 + <total git commit count>
```

Set in `build.gradle:36`. Ensures monotonic increase > 1380. Current: `51372`.

### GitHub release tag

```
v<versionName>
```

The CI workflow (`app.yml`) extracts this from the APK filename with:
```
sed -E 's/^shizuku-(.*[-.](debug[0-9]+|release[0-9]+))-[^.]+\.apk/\1/'
```

**Example tag:** `v13.7.0-thedjchi+stayturgid-release11`

### Release history

| Tag | APK | Changes |
|---|---|---|
| `v13.7.0-thedjchi+stayturgid-release25` | `shizuku-v13.7.0-thedjchi+stayturgid-release25-universal.apk` | **Current.** BootRetryWorker retries indefinitely instead of capping at 5 attempts (#7); widened post-tcpip reconnect window; signing-cert trust allowlist for silent grant loss (#4); CI release signing secrets configured, H1 resolved (#5); idempotent tag/release creation in CI (#9). |
| `v13.7.0-thedjchi+stayturgid-release15` | `shizuku-v13.7.0-thedjchi+stayturgid-release15-universal.apk` | HeadlessLogger everywhere (AdbStartWorker, state machine, ProvisionAuth). Complete boot→running traceability. |
| `v13.7.0-thedjchi+stayturgid-release13` | `shizuku-v13.7.0-thedjchi+stayturgid-release13-universal.apk` | HeadlessLogger, setResult feedback, boot retry, API docs. |
| `v13.7.0-thedjchi+stayturgid-release11` | `shizuku-v13.7.0-thedjchi+stayturgid-release11-universal.apk` | Build metadata format (`+`). versionCode > 1380. |
| `v13.7.0-thedjchi-stayturgid-release10` | `shizuku-v13.7.0-thedjchi-stayturgid-release10-universal.apk` | UNKNOWN launch mode treated as ADB. |
| `v13.7.0-thedjchi-stayturgid-release8` | `shizuku-v13.7.0-thedjchi-stayturgid-release8-universal.apk` | Obtainium compat: versionName matches tag. |
| `v13.7.0-thedjchi-stayturgid-release7` | `shizuku-v13.7.0-thedjchi-stayturgid-release7-universal.apk` | HEADLESS_STATUS, startDirect retry. |

All releases are published (not drafts). Only the latest matters for new work.

---

## Implemented features

### Headless receivers (`HeadlessStartStopReceiver.kt`)

In `manager/src/main/java/moe/shizuku/manager/receiver/`. Protected by `INTERACT_ACROSS_USERS_FULL` permission — only ADB shell or system can trigger.

**`HEADLESS_START`** — starts Shizuku server via ADB without UI. Flow:
1. If `launchMode` is `ADB` or `UNKNOWN` (fresh install), calls `tryEnsureWirelessAdb()` then `AdbStarter.startDirect()` with the configured TCP port
2. If `launchMode` is `ROOT`, falls through to `ShizukuReceiverStarter.start()`
3. `tryEnsureWirelessAdb()` sets `adb_wifi_enabled` and `adb_enabled` via `Settings.Global` if `WRITE_SECURE_SETTINGS` is held
4. `startDirect()` retries up to 3 times with 5s delays (configurable)

**`HEADLESS_STOP`** — calls `Shizuku.exit()` to stop the server.

**`HEADLESS_STATUS`** — returns structured state. Usage:
```bash
adb shell am broadcast -a moe.shizuku.privileged.api.HEADLESS_STATUS moe.shizuku.privileged.api
# Result: result=<stateCode>, data="RUNNING (binder=true, ADB: USB:1, v13.7.0-thedjchi+stayturgid-release11)"
```
Extras: `state`, `binder_alive`, `adb_tcp_port`, `adb_wifi_enabled`, `adb_enabled`, `version_name`, `version_code`.

Replaces `pgrep -f shizuku_server` and `ss -tlnp` in fleet health probes.

### Provision auth receiver (`ProvisionAuthReceiver.kt`)

Seeds the auth token used by authenticated start/stop intents. Usage:
```bash
adb shell am broadcast -a moe.shizuku.privileged.api.PROVISION_AUTH \
    -e auth_token "YOUR_TOKEN"
```

### Fleet profile (`FleetProfileActivity.kt` + `FleetProfileApplier.kt`)

JSON configuration profile for settings without UI. Following AutoJs6's `FleetProfileActivity` pattern:
```bash
adb shell am start -a moe.shizuku.privileged.api.APPLY_FLEET_PROFILE \
    -e profile_path /path/to/profile.json \
    moe.shizuku.manager.fleet.FleetProfileActivity
```

Settings supported: `mode`, `start_on_boot`, `watchdog`, `tcp_mode`, `tcp_port`, `auto_disable_usb_debugging`, `legacy_pairing`, `update_mode`. Written to `ShizukuSettings` shared prefs.

Default profile bundled at `assets/fleet_profile_default.json`.

### Auth bypass in `AuthenticatedReceiver.kt`

Shell/root callers and apps holding `START_STOP_SERVER` permission skip the auth token check.

### Trusted signer allowlist (`ShizukuConfigManager.java`)

Added 2026-07-31 (PR #4) to work around a silent-grant-loss bug: `ShizukuConfigManager`'s
constructor reconciles persisted per-app grants against currently-installed packages on
every server start, and can silently drop a UID's grant on a transient package-set read
mismatch — hit in production against `org.stayturgid.agent` (grant reset to "Authorized 0
applications" from nothing more than a local `shizuku_server` restart). `find(uid)` now
checks a hardcoded allowlist of trusted APK signing-certificate SHA-256 fingerprints
before consulting the persisted config, returning a synthetic always-allowed entry on a
match. Full rationale (including why "trust whoever signed the running Shizuku build" was
rejected) in `docs/trusted-signer-allowlist.md`.

### HeadlessLogger (`HeadlessLogger.kt`)

File + logcat logger for all headless operations. Writes to
`/sdcard/Android/data/moe.shizuku.privileged.api/files/headless.log`
in Unix-style format (`YYYY-MM-DD HH:MM:SS LEVEL component: message`).
Also logged to `logcat -s ShizukuHeadless`.

### Boot retry (`BootRetryWorker.kt`)

WorkManager worker scheduled by `BootCompleteReceiver` with 30s delay and
`NOT_ROAMING` network constraint. Addresses slow WiFi startups where Shizuku
fails at boot but WiFi comes up seconds later. Retries indefinitely (no
attempt cap since PR #7, 2026-07-31) via WorkManager's own exponential
backoff (capped ~5h/attempt) — a device blocked on a human action at boot
(FBE unlock, new-network authorization) may not get attention for hours or
days, so giving up permanently after a handful of quick retries meant
Shizuku would never try again until the next full reboot.

### Direct ADB start in `AdbStarter.kt`

`startDirect(context, port, maxRetries=3, retryDelayMs=5000)` — fire-and-forget coroutine that wraps `startAdb()` with retry logic. Bypasses `getAdbTcpPort()` (which returns -1 for Android 11+ wireless ADB — it reads `service.adb.tcp.port` system property, not the `adb_wifi_enabled` setting used by wireless debugging).

---

## Build infrastructure

### CI workflow (`.github/workflows/app.yml`)

Inherited from upstream, modified:
- **Manual trigger** (`workflow_dispatch`) — no auto-trigger on push
- Installs Android SDK + CMake 3.31.0 + NDK 29 + Ninja
- `debug=true` → `assembleDebug`, uploads artifact (no signing needed)
- `debug=false` → `assembleRelease`, creates published GitHub Release (requires `KEYSTORE` secrets)

**Resolved 2026-07-31 (H1, PR #5):** `KEYSTORE`/`KEYSTORE_PASSWORD`/`KEYSTORE_ALIAS`/`KEYSTORE_ALIAS_PASSWORD`
are now configured as GitHub Actions secrets on this repo (dedicated release keystore,
independent of stayturgid-agent's own signing key; backed up in 1Password + secretspec,
see `secretspec.toml`). CI release builds are signed properly instead of falling back to
the shared Android debug keystore. Verified via a real `workflow_dispatch` run.

### HeadlessLogger

Writes to `headless.log` at `context.getExternalFilesDir(null)` (typically
`/sdcard/Android/data/moe.shizuku.privileged.api/files/headless.log`) AND
logcat tag `ShizukuHeadless`. Initialized in `ShizukuApplication.onCreate()`.

Format: `YYYY-MM-DD HH:MM:SS LEVEL Component: message`

### Boot retry

`BootCompleteReceiver` schedules a `BootRetryWorker` (WorkManager, 30s delay,
`NOT_ROAMING` constraint). If WiFi wasn't ready at boot, retries `ShizukuReceiverStarter.start()`.

### Build

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11 ./gradlew :manager:assembleRelease
ls manager/build/outputs/apk/release/
# Requires: JDK 21, build-tools;36.0.0, platforms;android-36, cmake;3.31.0, ndk;29.0.14206865, ninja
```

---

## Build fixes applied

| Issue | Fix |
|---|---|
| NDK version mismatch | `29.0.14206865` (prebuilt boringssl/libcxx needed LLVM 18, NDK 26 had LLVM 17) |
| Kotlin JVM target mismatch | `kotlinOptions { jvmTarget = "21" }` in `server/build.gradle` |
| Missing import | `AdbStarter` import in `ShizukuReceiverStarter.kt` |

---

## Key design decisions

1. **`getAdbTcpPort()` is wrong for Android 11+** — reads `service.adb.tcp.port` system property. Android 11+ wireless ADB uses `adb_wifi_enabled` setting, not system properties. Always returns -1. Headless start bypasses it by calling `startDirect` with the configured TCP port directly.

2. **Wireless ADB recovery built in** — `tryEnsureWirelessAdb()` sets `adb_wifi_enabled` and `adb_enabled` via `WRITE_SECURE_SETTINGS` before connecting. Eliminates stayturgid's separate recovery in `shizuku.js`.

3. **UNKNOWN launch mode** — Fresh Shizuku installs have launch mode = UNKNOWN. Both `HeadlessStartStopReceiver` and `ShizukuReceiverStarter` now treat UNKNOWN as ADB for headless start, so it works without opening the UI first.

4. **Samsung process freezer** — Samsung's battery optimization freezes the broadcast receiver on first install. Requires opening the Shizuku app and tapping "Start" once. After that, `HEADLESS_*` broadcasts work and persist across reboots.

5. **Fleet profile vs stayturgid needs** — historically stayturgid configured AutoJs6 settings (not Shizuku settings) via AutoJs6's fleet profile; AutoJs6 has since been retired fleet-wide in favor of the native agent (`device/native-agent/`), which now owns ongoing operational configuration directly rather than through a fleet-profile JSON. Shizuku's own fleet profile remains for initial provisioning only, not ongoing operations.

---

## Known issues & limitations

| Issue | Details |
|---|---|
| Samsung process freezer | First boot after install: broadcast receiver frozen until app launched once. Device issue, no code fix. |
| FleetProfileApplier limited | Only covers 8 Shizuku settings. Not tested on real device. |
| mDNS port discovery | Headless start uses configured TCP port directly; if ADB picked a different port, connection fails silently. |
| No upstream sync strategy | If `thedjchi/Shizuku` releases v13.8.0, this fork needs manual rebase. No automation. |

Resolved since this table was last pruned: "Fork CI can't sign release APKs" (H1, PR #5,
2026-07-31 — CI signing secrets now configured) and "PROVISION_AUTH ↔ shizuku.json two
parallel auth mechanisms" (M2, resolved 2026-07-31 — the premise was wrong; see
`OPTIONS.md`'s Resolved table for the full explanation, fixed stayturgid-side in
stayturgid PR #164, no fork-side code change needed).

---

## Useful commands

```bash
# Build release APK
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11 ./gradlew :manager:assembleRelease

# Upload to GitHub
gh release create v13.7.0-thedjchi+stayturgid-release<N> out/apk/*.apk --repo djbclark/Shizuku

# On device: start Shizuku headlessly
adb shell am broadcast -a moe.shizuku.privileged.api.HEADLESS_START moe.shizuku.privileged.api

# On device: check status
adb shell am broadcast -a moe.shizuku.privileged.api.HEADLESS_STATUS moe.shizuku.privileged.api

# On device: stop Shizuku
adb shell am broadcast -a moe.shizuku.privileged.api.HEADLESS_STOP moe.shizuku.privileged.api

# Provision auth token
adb shell am broadcast -a moe.shizuku.privileged.api.PROVISION_AUTH -e auth_token "mytoken" moe.shizuku.privileged.api

# Apply fleet profile
adb shell am start -a moe.shizuku.privileged.api.APPLY_FLEET_PROFILE \
    -e profile_path /sdcard/Download/fleet.json \
    moe.shizuku.manager.fleet.FleetProfileActivity
```
