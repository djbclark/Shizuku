# HANDOFF — Shizuku fleet/headless features

Last updated: **2026-07-11**

---

## Fork & release convention

All work lives on **djbclark/Shizuku** (fork of thedjchi/Shizuku). Pre-built APKs and source tags follow this naming:

```
shizuku-v<version>-stayturgid-<buildType><N>-<abi>.apk
```

| Part | Meaning | Example |
|---|---|---|
| `version` | Upstream base version + fork suffix | `13.7.0-thedjchi` |
| `stayturgid` | Fork identifier | — |
| `buildType` | `release` or `debug` | — |
| `N` | Commits since upstream `v13.7.0-thedjchi` tag | `5` |
| `abi` | Target ABI (`universal` when unsplit) | `universal` |

**Current release:** `shizuku-v13.7.0-thedjchi-stayturgid-release5-universal.apk`
**Tag:** `v13.7.0-thedjchi-stayturgid-release5`
**Release:** https://github.com/djbclark/Shizuku/releases/tag/v13.7.0-thedjchi-stayturgid-release5

The counter `N` auto-increments with each commit (computed from `git rev-list --count v13.7.0-thedjchi..HEAD` in `manager/build.gradle`). The CI workflow (`.github/workflows/app.yml`) uses this naming for both artifacts and release tags.

---

## Session summary

Implemented fleet/headless automation features on the `thedjchi/Shizuku` fork (v13.7.0-thedjchi base). The work was driven by real fleet-deployment needs from the [stayturgid](https://github.com/djbclark/stayturgid) project, which manages a fleet of Android devices using AutoJs6 + Shizuku.

---

## What was implemented

### New files

| File | Purpose |
|---|---|
| `manager/.../receiver/HeadlessStartStopReceiver.kt` | Shell-only `HEADLESS_START` / `HEADLESS_STOP` broadcasts. Protected by `INTERACT_ACROSS_USERS_FULL`. On start, auto-enables ADB over WiFi via `WRITE_SECURE_SETTINGS`, then calls startDirect on the configured TCP port (bypasses broken `getAdbTcpPort()` system-property check on Android 11+). |
| `manager/.../receiver/ProvisionAuthReceiver.kt` | Shell-only `PROVISION_AUTH` broadcast to seed a known auth token via `ShizukuSettings.setAuthToken()` (for authenticated-intent workflow). |
| `manager/.../fleet/FleetProfileActivity.kt` | No-UI activity (Theme.NoDisplay) that applies a JSON configuration profile. Follows the AutoJs6 `FleetProfileActivity` pattern. |
| `manager/.../fleet/FleetProfileApplier.kt` | JSON → SharedPreferences writer. Handles type coercions (mode/update_mode string aliases, tcp_port int→string, etc.) and context-dependent setters (start_on_boot, watchdog). |
| `manager/src/main/assets/fleet_profile_default.json` | Bundled reference profile with recommended fleet defaults (start_on_boot, watchdog, tcp_mode, etc.). |

### Modified files

| File | What changed |
|---|---|
| `AndroidManifest.xml` | Registered `HeadlessStartStopReceiver`, `ProvisionAuthReceiver`, `FleetProfileActivity`. Added `START_STOP_SERVER` permission. |
| `AdbStarter.kt` | Added `startDirect(context, port)` — fire-and-forget coroutine wrapper around `startAdb()`, so headless start doesn't block the receiver. |
| `AuthenticatedReceiver.kt` | Shell/root callers and `START_STOP_SERVER` holders bypass auth token check. |
| `ShizukuReceiverStarter.kt` | Added import for `AdbStarter`. On ADB mode with TCP port available, calls `startDirect()` (the headless path). |
| `ShizukuSettings.java` | Added `setAuthToken(String)` for provisioning. |
| `strings.xml` | Strings for the new `START_STOP_SERVER` permission. |
| `build.gradle` (root) | Downgraded compileSdk/targetSdk to 36. Updated NDK to 29.0.14206865 (needed for prebuilt boringssl/libcxx LLVM 18 compat). |
| `server/build.gradle` | Added `kotlinOptions { jvmTarget = "21" }` (fixes JVM target mismatch). |
| `manager/build.gradle` | Changed APK naming to AutoJs6 convention: `shizuku-v<version>-<abi>.apk` (was `shizuku-v<version>-<variant>.apk`). |
| `README.md` | Restructured headless/fleet docs into three-phase lifecycle: Provisioning → Boot → Remote. |

### Build fixes

- **NDK version**: `29.0.14206865` (prebuilt libs needed LLVM 18, NDK 26 had LLVM 17)
- **Kotlin JVM target**: Added `jvmTarget = "21"` to `server/build.gradle` (matched Java 21 target)
- **Missing import**: Added `moe.shizuku.manager.adb.AdbStarter` import to `ShizukuReceiverStarter.kt`

---

## Key design decisions

1. **Headless start bypasses `getAdbTcpPort()`** — On Android 11+, wireless ADB uses the `adb_wifi_enabled` setting, not the `service.adb.tcp.port` system property. So `getAdbTcpPort()` returns -1 even when ADB is working. The headless receiver calls `startDirect(context, ShizukuSettings.getTcpPort())` directly instead of `ShizukuReceiverStarter.start()` (which checks the system property).

2. **Wireless ADB recovery** — The `HeadlessStartStopReceiver.tryEnsureWirelessAdb()` sets `adb_wifi_enabled = 1` and `adb_enabled = 1` via `Settings.Global` before connecting. This eliminates the need for stayturgid's external recovery in `shizuku.js`.

3. **Fleet profile not needed by stayturgid** — stayturgid configures AutoJs6 settings (not Shizuku settings) via AutoJs6's `FleetProfileActivity`. The Shizuku fleet profile is useful for initial device provisioning (one-time setup) but not for ongoing operations. It was still implemented as a parallel feature.

4. **No auth token needed** — stayturgid uses `pm grant` + `shizuku.json` patching, not auth tokens. The `ProvisionAuthReceiver` is for a different workflow (automation apps that use the authenticated intents).

---

## Remaining considerations

### CI/CD

The existing `.github/workflows/app.yml` workflow (inherited from upstream) handles builds and releases:
- **Manual trigger only** (`workflow_dispatch`) — no auto-trigger on push
- `debug=true` builds `assembleDebug` and uploads artifact (no signing needed)
- `debug=false` builds `assembleRelease` → creates a draft GitHub Release with the APK (requires `KEYSTORE` secrets)
- Updated to install Android SDK components (`build-tools;36.0.0`, `platforms;android-36`, `cmake;3.31.0`, `ndk;29.0.14206865`) and Ninja
- APK naming and tag extraction updated to match the `stayturgid-releaseN` convention

### Already implemented (2026-07-11)

- **HEADLESS_STATUS broadcast** (`HeadlessStartStopReceiver`) — Returns structured state info via `am broadcast` extras: `state` (RUNNING/STOPPED/etc.), `binder_alive`, `adb_tcp_port`, `adb_wifi_enabled`, `adb_enabled`, `version_name`, `version_code`. Replaces `pgrep -f shizuku_server` in JS watchdog and Python health probes.
- **Retry logic in `startDirect()`** — `AdbStarter.startDirect()` now retries `startAdb` up to 3 times with 5s delays (configurable via `maxRetries`/`retryDelayMs` params). Addresses silent failures on slow boots where WiFi or ADB isn't ready yet.

### Potential improvements

1. **Boot receiver reliability** — The `BootCompleteReceiver` starts Shizuku on boot, but on Android 11+ with wireless ADB, it depends on WiFi being available. When WiFi isn't available yet (e.g., device boots without WiFi), the start fails. The receiver could be enhanced to retry when WiFi connects.

2. **Watchdog notification channel** — The watchdog service currently stops unexpectedly when ADB disconnects (e.g., USB plug/unplug). The state machine transitions to CRASHED but recovery requires the headless start or boot receiver. Consider making the watchdog more resilient.

3. **mDNS-free start** — The current `AdbStartWorker` (interactive path) uses mDNS to discover the wireless ADB port on Android 11+. For headless, the receiver uses the configured TCP port directly. If the port is wrong or ADB uses a different port, the connection fails. Could add fallback port discovery.

4. **Auth token integration with stayturgid** — stayturgid patches `shizuku.json` directly. The `ProvisionAuthReceiver` provides an alternative path but isn't used. Could bridge these by having the receiver also patch `shizuku.json`.

### Testing needs

The following should be verified on device:

- `adb shell am broadcast ... HEADLESS_START` works with and without wireless ADB pre-enabled
- `adb shell am broadcast ... HEADLESS_STOP` stops the server cleanly
- `adb shell am broadcast ... PROVISION_AUTH -e auth_token "x"` persists the token
- `adb shell am start ... APPLY_FLEET_PROFILE -e profile_path ...` applies settings
- `start_on_boot` starts Shizuku after reboot
- Build reproducibility via the CI workflow

---

## Files that may need attention

| File | Why |
|---|---|
| `manager/.../adb/AdbStarter.kt` | `startDirect` uses `Dispatchers.IO` — works for fire-and-forget but error handling is just logging. Consider retry logic. |
| `manager/.../receiver/HeadlessStartStopReceiver.kt` | `tryEnsureWirelessAdb` is best-effort. No feedback to caller if ADB recovery fails. |
| `manager/.../fleet/FleetProfileApplier.kt` | New code, limited testing. Only covers Shizuku's 8 relevant settings. |
| `.github/workflows/app.yml` | Fork doesn't have `KEYSTORE` secrets configured, so release builds (debug=false) will fail the signing step. Only debug builds work on the fork. |

## Release history (djbclark/Shizuku)

| Tag | APK | Notes |
|---|---|---|
| `v13.7.0-thedjchi-stayturgid-release7` | `shizuku-v13.7.0-thedjchi-stayturgid-release7-universal.apk` | **Current** — HEADLESS_STATUS, retry logic in startDirect |
| `v13.7.0-thedjchi-stayturgid-release5` | `shizuku-v13.7.0-thedjchi-stayturgid-release5-universal.apk` | Initial fleet/headless features |
