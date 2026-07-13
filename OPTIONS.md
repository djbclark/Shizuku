# OPTIONS — Open work items

Last updated: **2026-07-11**

---

## High priority

### H1 — CI signing secrets
Configure `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASSWORD` in `djbclark/Shizuku` → Settings → Secrets → Actions. Without these, CI release builds fail silently. Currently only manual debug-keystore builds work.

### H2 — Samsung process freezer workaround
On first install, Samsung's battery optimization freezes the broadcast receiver. Requires opening Shizuku app once and tapping "Start". After that, `HEADLESS_*` broadcasts work across reboots. No known code fix — this is an OEM behavior.

### H3 — Boot receiver with WiFi readiness
`BootCompleteReceiver` + `BootRetryWorker` covers most cases, but on some devices WiFi takes 30+ seconds after boot. The current retry has a single 30s delay. If WiFi still isn't up, no further retries. Could add exponential backoff or a connectivity change listener.

---

## Medium priority

### M1 — mDNS port discovery fallback for headless
`AdbStartWorker` (interactive path) uses mDNS to discover wireless ADB ports on Android 11+. The headless `startDirect()` path uses the configured TCP port (default 5555) directly. If ADB picked a different port, the connection fails. Could integrate `AdbMdns` into the headless flow.

### M2 — PROVISION_AUTH ↔ shizuku.json bridge
stayturgid patches `/data/local/tmp/shizuku/shizuku.json` directly; `ProvisionAuthReceiver` writes to SharedPreferences. Two parallel auth mechanisms. Unbridgeable from the app process (different privilege levels) — would need the Shizuku server to proxy writes to `/data/local/tmp/`.

### M3 — FleetProfileApplier testing
New code, 8 settings supported, never tested on a real device. Need to verify JSON parsing, type coercions, and context-dependent setters (start_on_boot, watchdog) work end-to-end.

### M4 — Watchdog resilience on ADB disconnect
When USB is plugged/unplugged, the ADB connection drops. The state machine goes to CRASHED but the watchdog doesn't attempt automatic recovery. Could add a retry after CRASHED transitions.

---

## Low priority

### L1 — Upstream sync strategy
If `thedjchi/Shizuku` or `RikkaApps/Shizuku` releases v13.8.0, this fork needs manual rebase. No documented process or automation. Standard approach: rebase on top of new upstream tag, resolve conflicts, rebuild.

### L2 — Automated tests
No unit or integration tests for new features. Kotlin test framework (`kotlin.test`) or Android instrumentation tests (`androidTest`) could verify receivers, logger, and state machine behavior.

### L3 — HEADLESS_STATUS output format
Currently returns human-readable summary + extras Bundle. Could add a structured JSON format for easier machine parsing by fleet health probes.

### L4 — Log rotation
`HeadlessLogger` keeps a single `headless.log` with a `.1` backup at 256KB. On long-running devices, this could fill up. Could add a config option or more rotation files (`.2`, `.3`, etc.).

### L5 — README Obtainium setup guide
README documents the API but doesn't explain how to add this fork as a source in Obtainium. Could add a step-by-step guide with the release URL.

---

## Resolved (2026-07-11)

| Issue | Resolution |
|---|---|
| No feedback from headless operations | `setResult()` codes on all broadcasts |
| Boot receiver doesn't retry | `BootRetryWorker` via WorkManager (30s, network constraint) |
| No logging | `HeadlessLogger` — file + logcat, Unix format |
| HEADLESS_STATUS format | Added `log_path`, clean keys, human-readable summary |
| AdbStartWorker not logged | Logs all stages: enqueue → ADB enable → mDNS → start → errors |
| State machine not logged | `transition()` writes to HeadlessLogger |
| ProvisionAuth not logged | Full logging of token provisioning |
| UNKNOWN launch mode blocks headless | Treated as ADB in both HeadlessStartStopReceiver and ShizukuReceiverStarter |
| Obtainium "pseudo-version" | Version format changed to `13.7.0-thedjchi+stayturgid-releaseN` (build metadata) |
| APK naming convention | Standardized to `shizuku-v<version>-<abi>.apk` matching AutoJs6 pattern |
| versionCode too low | Offset 50000 added to git commit count (currently 51376) |
