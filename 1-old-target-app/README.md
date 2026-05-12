# Sample Android app with deliberate migration issues

A tiny synthetic project for smoke-testing `apply_checklist.py`. Every file
below contains code or manifest entries that intentionally violate one or
more rules in `out/checklist.jsonl`.

## Run

From the repo root:

```sh
# Target SDK 30 — only earlier-API rules fire
py apply_checklist.py --root sample-app --target-sdk 30 --min-sdk 24 \
    --out sample-app/migration-report-target30.md

# Target SDK 34 — most rules fire
py apply_checklist.py --root sample-app --target-sdk 34 --min-sdk 24 \
    --out sample-app/migration-report-target34.md

# Target SDK 35 — everything including A15 rules fires
py apply_checklist.py --root sample-app --target-sdk 35 --min-sdk 24 \
    --out sample-app/migration-report-target35.md
```

### Try auto-fix on this sample

```sh
# See what would change (no writes)
py apply_checklist.py --root sample-app --target-sdk 34 --min-sdk 24 --auto-fix --dry-run

# Actually apply the fixes (writes to AndroidManifest.xml)
py apply_checklist.py --root sample-app --target-sdk 34 --min-sdk 24 --auto-fix
```

The current auto-fix set handles four manifest-level rules:

| Rule | Fix |
|---|---|
| `A9-FOREGROUND-SERVICE-PERMISSION` | Adds `<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>` |
| `A10-USE-FULL-SCREEN-INTENT-REQUIRED` | Adds `<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>` |
| `A11-MAPS-V1-REMOVED` | Removes the `<uses-library android:name="com.google.android.maps"/>` line |
| `A13-POST-NOTIFICATIONS-PERMISSION` | Adds `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` |

After running `--auto-fix`, the manifest gains three new permission lines and
loses the Maps v1 declaration. Re-running the scanner without `--auto-fix`
will show those four rules no longer firing (rule hit count drops 37 → 32 at
target-sdk 34).

To restore the deliberately-broken state for another demo, `git checkout
sample-app/app/src/main/AndroidManifest.xml`.

## What's intentionally wrong, and which rule should catch it

### `app/build.gradle`

| Issue | Rule |
|---|---|
| `targetSdkVersion 30` — also serves as the test knob | (changes which rules apply) |

### `app/src/main/AndroidManifest.xml`

| Issue | Rule |
|---|---|
| `<activity>`, `<service>`, `<receiver>` with `<intent-filter>` but no `android:exported` | `A12-ANDROID-EXPORTED-REQUIRED` |
| `<service>` has no `android:foregroundServiceType` while code calls `startForeground()` | `A14-FGS-TYPES-REQUIRED` |
| Code uses `BluetoothAdapter` but no `BLUETOOTH_SCAN`/`_CONNECT` permissions | `A12-NEW-BLUETOOTH-PERMISSIONS`, `A14-BLUETOOTH-CONNECT-ENFORCED` |
| `startForeground()` called but no `FOREGROUND_SERVICE` permission | `A9-FOREGROUND-SERVICE-PERMISSION` |
| `setExact()` called but no `SCHEDULE_EXACT_ALARM` permission | `A12-EXACT-ALARM-PERMISSION` |
| `setFullScreenIntent()` called but no `USE_FULL_SCREEN_INTENT` | `A10-USE-FULL-SCREEN-INTENT-REQUIRED` |
| Notifications posted but no `POST_NOTIFICATIONS` permission | `A13-POST-NOTIFICATIONS-PERMISSION`, `A13-FGS-NOTIF-PERMISSION-VISIBILITY` |
| `READ_EXTERNAL_STORAGE` declared but no `READ_MEDIA_IMAGES`/`_VIDEO`/`_AUDIO` | `A13-GRANULAR-MEDIA-PERMISSIONS` |
| `<uses-library android:name="com.google.android.maps"/>` | `A11-MAPS-V1-REMOVED` |

### `app/src/main/java/com/example/MainActivity.kt`

| Issue | Rule |
|---|---|
| `Build.SERIAL` reference | `A9-BUILD-SERIAL-UNAVAILABLE` |
| `PendingIntent.getActivity(...)` without `FLAG_IMMUTABLE`/`FLAG_MUTABLE` | `A12-PENDING-INTENT-MUTABILITY` |
| `setNdefPushMessage(...)` | `A10-ANDROID-BEAM-DEPRECATED` |
| `BluetoothAdapter.enable()` | `A13-BLUETOOTH-ENABLE-DISABLE-DEPRECATED` |
| `WebSettings.setForceDark(...)` | `A13-WEBVIEW-FORCE-DARK-DEPRECATED` |
| `WebSettings.setDatabaseEnabled(true)` | `A15-DEPRECATE-WEBSQL` |
| `AlarmManager.setExact(...)` | `A12-EXACT-ALARM-PERMISSION`, `A14-EXACT-ALARM-DENIED-BY-DEFAULT` |
| `Intent("com.example.ACTION_ONLY")` (action-string-only ctor) | `A14-IMPLICIT-INTENTS-EXPLICIT-ONLY` |
| `Notification.Builder.setFullScreenIntent(...)` | `A10-USE-FULL-SCREEN-INTENT-REQUIRED` |
| `activityManager.killBackgroundProcesses("other.pkg")` | `A14-KILLBG-OWN-ONLY` |
| `setForceDark` import | `A13-WEBVIEW-FORCE-DARK-DEPRECATED` |
| `Display.getRealMetrics(...)` | `A12-DISPLAY-GETREAL-DEPRECATED` |

> Note: `A11-CUSTOM-TOAST-FROM-BG-BLOCKED` exists in the catalog but its doc
> heading ("Custom toasts from the background are blocked") is at H4 in the
> source page, which is outside the H1–H3 enumeration. The rule won't
> currently match — kept as an example of where extending enumeration depth
> would help.

### `app/src/main/java/com/example/SyncService.kt`

| Issue | Rule |
|---|---|
| `startForeground(...)` with no manifest `foregroundServiceType` and no `FOREGROUND_SERVICE` permission | `A14-FGS-TYPES-REQUIRED`, `A9-FOREGROUND-SERVICE-PERMISSION` |
| `BluetoothAdapter.getProfileConnectionState(...)` with no `BLUETOOTH_CONNECT` | `A14-BLUETOOTH-CONNECT-ENFORCED` |
| `registerReceiver(...)` with no `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED` flag | `A14-RECEIVER-EXPORTED-FLAG` |
| `notify(...)` with no `POST_NOTIFICATIONS` declared | `A13-POST-NOTIFICATIONS-PERMISSION` |

### `app/src/main/java/com/example/BootReceiver.kt`

| Issue | Rule |
|---|---|
| `BroadcastReceiver` invoked from `BOOT_COMPLETED` that calls `startActivity(...)` | `A12-NOTIFICATION-TRAMPOLINES-BLOCKED` (trampoline shape), `A15-BOOT-COMPLETED-FGS-RESTRICTIONS` |

### `app/src/main/res/xml/network_security_config.xml`

| Issue | Rule |
|---|---|
| `<pin digest="SHA-1">` | `A10-TLS-SHA1-CERTS-UNTRUSTED` |

## Expected hit counts

These are the totals the runner currently surfaces on this sample:

| `--target-sdk` | Rules with findings |
|---|---:|
| 30 | 22 |
| 34 | 37 |
| 35 | 37 |

(Some hits are `MANUAL` entries for rules that are `not_statically_detectable` —
e.g. `A11-APK-SIG-V2-REQUIRED` always shows up because the runner can't
verify signing config from source.)

Exact numbers will drift as you tighten regexes in `out/_catalog.py`.

## Caveats

- Several rules are `not_statically_detectable` (16 KB pages, APK signing,
  TEXTREL). The runner emits a "manual review" entry — those will appear
  in the report even though this sample has no `.so` files.
- A few rules are detectable in principle but rely on shapes this sample
  doesn't exercise (e.g. `MediaProjection` reuse, dynamic code loading).
