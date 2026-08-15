# OPTIONS — Open work items

Last updated: **2026-07-31**

---

## High priority

### H2 — Samsung process freezer workaround
On first install, Samsung's battery optimization freezes the broadcast receiver. Requires opening Shizuku app once and tapping "Start". After that, `HEADLESS_*` broadcasts work across reboots. No known code fix — this is an OEM behavior.

---

## Medium priority

### M1 — mDNS port discovery fallback for headless
`AdbStartWorker` (interactive path) uses mDNS to discover wireless ADB ports on Android 11+. The headless `startDirect()` path uses the configured TCP port (default 5555) directly. If ADB picked a different port, the connection fails. Could integrate `AdbMdns` into the headless flow.

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

## Resolved (2026-07-25)

| Issue | Resolution |
|---|---|
| Fire OS 8 in-place upgrade drops native libs | `useLegacyPackaging = false` in `manager/build.gradle` (commit `cf6d2092`, PR #1) — libs now stored uncompressed (STORED) instead of extracted-on-first-install, which Fire OS 8 skipped during in-place upgrades |
| Fire OS notification icon lookup | `drawable/ic_system_icon#no_obfuscate` added to `manager/aapt2-resources.cfg` (same commit) |
| H3 boot receiver retry (partial) | `BootRetryWorker` moved to exponential backoff (10s base, max 5 attempts, ~2.5min window), each attempt re-verifies `isRunning()` after a 3s delay |

## Resolved (2026-07-31)

| Issue | Resolution |
|---|---|
| H3 boot receiver retry — was marked "partial" above, verified fully resolved as of the morning of 2026-07-31, **then changed again the same day (see below)** | Re-read `BootRetryWorker.kt`/`BootCompleteReceiver.kt` directly: `MAX_ATTEMPTS = 5`, `VERIFY_DELAY_MS = 3000L`, and `setBackoffCriteria(EXPONENTIAL, 10, SECONDS)` all confirmed present and matching this table's own description exactly at the time — the "High priority" H3 open item above (describing a stale "single 30s delay, no further retries" behavior) was removed; it described the pre-fix state and was never updated after the fix landed |
| H3 boot receiver retry — superseding update, PR #7 (later on 2026-07-31) | The `MAX_ATTEMPTS = 5` cap confirmed just above was itself removed a few hours later: `BootRetryWorker` now retries **indefinitely** (WorkManager's own exponential backoff, ~5h/attempt cap), re-checking the start-on-boot setting on every attempt so a human explicitly disabling it is respected. Reason: a device blocked on a human action at boot (FBE unlock, new-network auth) may not get attention for hours/days, and the old 5-attempt cap exhausted within ~5 minutes, meaning Shizuku wouldn't retry again until the next full reboot — confirmed live against stayturgid issue #43 (hd8 CLOSED_NO_SHELL soak). Also widened the post-tcpip-mode-switch reconnect window (5→10 attempts, ~10s→~45s) since some devices needed more time for adbd to restart into TCP mode. Published as release25. |
| Signing-certificate trust allowlist — new item, not previously tracked here (PR #4) | `ShizukuConfigManager.find(uid)` silently drops a UID's Shizuku grant on any package-set read mismatch (not just uninstall/reinstall) — hit directly against production `org.stayturgid.agent` (grant reset to "Authorized 0 applications" from a plain local `shizuku_server` restart, no reboot/reinstall). Fixed by checking a hardcoded allowlist of trusted APK signing-certificate SHA-256 fingerprints in `find()` before consulting the persisted config. See `docs/trusted-signer-allowlist.md` for full rationale. |
| H1 — CI signing secrets | Generated a dedicated release keystore (RSA 4096, 30yr validity) independent of stayturgid-agent's own key, configured `KEYSTORE`/`KEYSTORE_PASSWORD`/`KEYSTORE_ALIAS`/`KEYSTORE_ALIAS_PASSWORD` as GitHub Actions secrets on `djbclark/Shizuku`. Verified via a real (non-debug) `workflow_dispatch` run — the `build` job compiled and signed successfully with the new keystore. Secrets backed up in two independent places (round-trip verified against the actual keystore file/password, not just trusted): a dedicated 1Password "Service" vault via a scoped service account (see `secretspec.toml`), and secretspec's own `onepassword://` provider pointed at that same vault. The repo has since moved to `frdminc/Shizuku` (2026-08-15) — the `djbclark/Shizuku` naming above is the state at the time; confirm the Actions secrets came across before the next release build. |
| M2 — PROVISION_AUTH ↔ shizuku.json bridge, description was based on a wrong premise | The "two parallel auth mechanisms" framing assumed shizuku.json patching was a real, working control lever that just needed bridging to `ProvisionAuthReceiver`'s SharedPreferences path. It never was: `ShizukuConfigManager`'s constructor unconditionally re-syncs every known app's config flags from the real Android `pm grant`/`pm revoke` state at every server startup, so shizuku.json is purely a cache of that state, not an independent lever — there was nothing to bridge. Confirmed via a live 4-step test on a real device (stayturgid#163) and fixed stayturgid-side in stayturgid PR #164: headless grants now go through `pm grant`/`pm revoke` + a conditional server restart, never shizuku.json. No fork-side code change needed here. |

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
