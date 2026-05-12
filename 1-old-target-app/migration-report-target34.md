# Migration findings for `C:\Users\CXNT48\BLOBS\AI-BASED-MIGRATIONS AND CODE GENERATION\prompt-comparison\best-of-breed\sample-app`

- Target SDK: **34**, Min SDK: **24**
- Rules in checklist: 82
- Applicable to this app: 78
- Rules with hits: **37**

## Summary by severity

| Severity | Rules hit |
|---|---:|
| BLOCKING | 20 |
| SILENT | 16 |
| DEPRECATION | 1 |
| LINT | 0 |

## BLOCKING

### A10-NDK-NO-TEXT-RELOCATIONS — Shared objects cannot contain text relocations (TEXTREL)

- **applies_when:** app bundles NDK shared libraries
- **failure_mode:** SELinux blocks .so load; app crashes
- **fix:** Rebuild native libraries with -fPIC and no text relocations.
- **doc:** https://developer.android.com/about/versions/10/behavior-changes-all#shared-objects-cannot-contain-text-relocations

> Manual review: Requires inspecting .so files for TEXTREL via readelf -d; not greppable from source.

```
# in Android.mk or CMakeLists.txt ensure -fPIC
```

### A11-APK-SIG-V2-REQUIRED — APK Signature Scheme v2+ required (v1-only signing rejected)

- **applies_when:** targetSdk >= 30
- **failure_mode:** App install/update rejected on Android 11+
- **fix:** Configure signing with v2 in addition to v1 in build.gradle / signing config.
- **doc:** https://developer.android.com/about/versions/11/behavior-changes-11#apk-signature-scheme-v2-now-required

> Manual review: Verifiable only by inspecting signing config or signed APK with apksigner.

```
signingConfigs { release { enableV2Signing true } }
```

### A11-MAPS-V1-REMOVED — Google Maps v1 shared library removed

- **applies_when:** manifest contains <uses-library android:name="com.google.android.maps"/>
- **failure_mode:** App fails on devices running Android 11+
- **fix:** Remove the <uses-library> entry and migrate to Maps SDK for Android.
- **doc:** https://developer.android.com/about/versions/11/behavior-changes-all#maps-v1-shared-library-removed

**Findings** (1):

- `!` `app/src/main/AndroidManifest.xml:19` — `<uses-library android:name="com.google.android.maps" android:required="false"/>`

```
<!-- remove <uses-library android:name="com.google.android.maps"/> -->
```

### A12-ANDROID-EXPORTED-REQUIRED — android:exported must be explicitly declared on components with intent filters

- **applies_when:** targetSdk >= 31 AND manifest has activity/service/receiver with <intent-filter>
- **failure_mode:** App fails to install: Manifest merger failure
- **fix:** Add android:exported="true"/"false" on every <activity>/<service>/<receiver> that has an <intent-filter>.
- **doc:** https://developer.android.com/about/versions/12/behavior-changes-12#safer-component-exporting

**Findings** (3):

- `!` `app/src/main/AndroidManifest.xml:26` — `<activity android:name=".MainActivity">`
- `!` `app/src/main/AndroidManifest.xml:35` — `<service android:name=".SyncService">`
- `!` `app/src/main/AndroidManifest.xml:43` — `<receiver android:name=".BootReceiver">`

```
<activity android:name=".X" android:exported="false">  <intent-filter>...</intent-filter></activity>
```

### A12-EXACT-ALARM-PERMISSION — Exact alarms require SCHEDULE_EXACT_ALARM permission

- **applies_when:** targetSdk >= 31 AND code uses setExact / setExactAndAllowWhileIdle / setAlarmClock
- **failure_mode:** SecurityException when scheduling exact alarm
- **fix:** Declare SCHEDULE_EXACT_ALARM (or USE_EXACT_ALARM on A13+) in manifest; check AlarmManager.canScheduleExactAlarms() before use.
- **doc:** https://developer.android.com/about/versions/12/behavior-changes-12#exact-alarm-permission

**Findings** (2):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:46` — `alarm.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60_000, pi)`
- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:47` — `alarm.setAlarmClock(AlarmManager.AlarmClockInfo(0L, pi), pi)`

```
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
```

### A12-FGS-BACKGROUND-START-BLOCKED — Cannot start foreground services from the background

- **applies_when:** targetSdk >= 31 AND code calls startForegroundService() from background
- **failure_mode:** ForegroundServiceStartNotAllowedException
- **fix:** Migrate background-initiated work to WorkManager expedited jobs; only call startForegroundService when an exempt trigger applies.
- **doc:** https://developer.android.com/about/versions/12/behavior-changes-12#foreground-service-launch-restrictions

**Candidates to review** (1):

- `?` `app/src/main/java/com/example/brokenapp/BootReceiver.kt:14` — `context.startForegroundService(Intent(context, SyncService::class.java))`

```
OneTimeWorkRequestBuilder<MyWorker>().setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
```

### A12-NEW-BLUETOOTH-PERMISSIONS — BLUETOOTH_SCAN / _ADVERTISE / _CONNECT runtime permissions replace legacy

- **applies_when:** targetSdk >= 31 AND code uses Bluetooth APIs
- **failure_mode:** SecurityException; missing runtime grant
- **fix:** Declare BLUETOOTH_SCAN/_CONNECT/_ADVERTISE in manifest; request at runtime; mark scan with usesPermissionFlags="neverForLocation" if applicable.
- **doc:** https://developer.android.com/about/versions/12/behavior-changes-12#bluetooth-permissions

**Findings** (4):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:11` — `import android.bluetooth.BluetoothAdapter`
- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:23` — `adapter: BluetoothAdapter, ws: WebSettings, display: Display,`
- `!` `app/src/main/java/com/example/brokenapp/SyncService.kt:8` — `import android.bluetooth.BluetoothAdapter`
- `!` `app/src/main/java/com/example/brokenapp/SyncService.kt:29` — `BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(0)`

```
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation"/>
```

### A12-PENDING-INTENT-MUTABILITY — PendingIntent must explicitly specify FLAG_IMMUTABLE or FLAG_MUTABLE

- **applies_when:** targetSdk >= 31 AND code creates PendingIntent
- **failure_mode:** IllegalArgumentException on PendingIntent creation
- **fix:** Pass FLAG_IMMUTABLE for outbound intents; FLAG_MUTABLE only when the system needs to fill EXTRAs.
- **doc:** https://developer.android.com/about/versions/12/behavior-changes-12#pending-intents-mutability

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:30` — `val pi = PendingIntent.getActivity(ctx, 0, Intent("X"), 0)`

```
PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
```

### A12-USES-NATIVE-LIBRARY-REQUIRED — Non-NDK vendor native libraries require <uses-native-library>

- **applies_when:** targetSdk >= 31 AND app dlopen()s a non-NDK vendor library
- **failure_mode:** dlopen() returns null; runtime failure
- **fix:** Add <uses-native-library android:name="libfoo.so" android:required="true|false"/> for every vendor library you depend on.
- **doc:** https://developer.android.com/about/versions/12/behavior-changes-12#vendor-supplied-native-shared-libraries

> Manual review: Which libraries are vendor-only is device-specific; cannot derive from source alone.

```
<uses-native-library android:name="libOpenCL.so" android:required="false"/>
```

### A13-GRANULAR-MEDIA-PERMISSIONS — READ_MEDIA_IMAGES/VIDEO/AUDIO replace READ_EXTERNAL_STORAGE

- **applies_when:** targetSdk >= 33 AND app accesses shared media files
- **failure_mode:** READ_EXTERNAL_STORAGE no longer grants media access
- **fix:** Add READ_MEDIA_IMAGES / READ_MEDIA_VIDEO / READ_MEDIA_AUDIO and request them at runtime when targeting 33+.
- **doc:** https://developer.android.com/about/versions/13/behavior-changes-13#granular-media-permissions

**Candidates to review** (2):

- `?` `app/src/main/AndroidManifest.xml:14` — `<!-- Missing: READ_MEDIA_IMAGES/_VIDEO/_AUDIO; only legacy READ_EXTERNAL_STORAGE is declared. -->`
- `?` `app/src/main/AndroidManifest.xml:15` — `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>`

```
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" android:maxSdkVersion="34"/>
```

### A13-POST-NOTIFICATIONS-PERMISSION — POST_NOTIFICATIONS runtime permission required

- **applies_when:** device runs API >= 33 AND app posts notifications
- **failure_mode:** Notifications silently dropped
- **fix:** Declare POST_NOTIFICATIONS in manifest; request at runtime with rationale UI.
- **doc:** https://developer.android.com/about/versions/13/behavior-changes-all#runtime-permission-for-notifications

**Findings** (2):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:53` — `val builder = Notification.Builder(ctx, "channel")`
- `!` `app/src/main/java/com/example/brokenapp/SyncService.kt:20` — `val notification: Notification = Notification.Builder(this, "ch")`

```
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

### A14-BLUETOOTH-CONNECT-ENFORCED — BluetoothAdapter.getProfileConnectionState() enforces BLUETOOTH_CONNECT

- **applies_when:** targetSdk >= 34 AND code calls BluetoothAdapter.getProfileConnectionState()
- **failure_mode:** SecurityException at runtime
- **fix:** Declare and request BLUETOOTH_CONNECT before calling the API.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-14#enforcement-of-bluetooth-connect-permission-in-bluetoothadapter

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/SyncService.kt:29` — `BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(0)`

```
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
```

### A14-FGS-TYPES-REQUIRED — Foreground services must declare an android:foregroundServiceType

- **applies_when:** targetSdk >= 34 AND code calls startForeground()
- **failure_mode:** MissingForegroundServiceTypeException at runtime
- **fix:** Declare android:foregroundServiceType in manifest AND pass a FOREGROUND_SERVICE_TYPE_* to startForeground().
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-14#foreground-service-types-are-required

**Findings** (1):

- `!` `app/src/main/AndroidManifest.xml:35` — `<service android:name=".SyncService">`

```
<service android:name=".MyService" android:foregroundServiceType="dataSync"/>
```

### A14-IMPLICIT-INTENTS-EXPLICIT-ONLY — Implicit intents only deliver to exported components; mutable PendingIntents need a component or package

- **applies_when:** targetSdk >= 34 AND code uses implicit Intent or mutable PendingIntent
- **failure_mode:** ActivityNotFoundException; exception on PendingIntent.send
- **fix:** Use explicit Intent (setPackage / setComponent) or mark target component exported=true.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-14#restrictions-to-implicit-and-pending-intents

**Candidates to review** (2):

- `?` `app/src/main/java/com/example/brokenapp/MainActivity.kt:30` — `val pi = PendingIntent.getActivity(ctx, 0, Intent("X"), 0)`
- `?` `app/src/main/java/com/example/brokenapp/MainActivity.kt:50` — `ctx.startActivity(Intent("com.example.ACTION_ONLY"))`

```
intent.setPackage(context.packageName)
```

### A14-MIN-TARGET-SDK-23 — Apps with targetSdkVersion < 23 cannot be installed

- **applies_when:** device runs API >= 34 AND app has targetSdk < 23
- **failure_mode:** INSTALL_FAILED_DEPRECATED_SDK_VERSION
- **fix:** Raise targetSdkVersion to at least 23; on A15 raise to 24.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-all#minimum-installable-target-api-level

**Candidates to review** (1):

- `?` `app/build.gradle:7` — `targetSdkVersion 30          // <-- change this when smoke-testing different rule sets`

```
defaultConfig { targetSdkVersion 34 }
```

### A14-RECEIVER-EXPORTED-FLAG — Context.registerReceiver must pass RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED

- **applies_when:** targetSdk >= 34 AND code calls Context.registerReceiver for non-system broadcasts
- **failure_mode:** SecurityException at runtime
- **fix:** Add ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED) (or EXPORTED) flag.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-14#runtime-registered-broadcasts-receivers-must-specify-export-behavior

**Candidates to review** (1):

- `?` `app/src/main/java/com/example/brokenapp/SyncService.kt:35` — `registerReceiver(recv, IntentFilter("com.example.brokenapp.PING"))`

```
ContextCompat.registerReceiver(ctx, recv, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
```

### A15-16KB-PAGE-SIZES — App must rebuild native libraries for 16 KB page sizes

- **applies_when:** device runs API >= 35 (16 KB device) AND app bundles .so libraries
- **failure_mode:** App fails to load native libraries on 16 KB devices
- **fix:** Rebuild with NDK r27+ and ensure ELF segments aligned to 16 KB; verify via APK Analyzer.
- **doc:** https://developer.android.com/about/versions/15/behavior-changes-all#support-for-16-kb-page-sizes

> Manual review: Detection requires inspecting .so ELF alignment; AGP / APK Analyzer flag this.

```
# in CMakeLists.txt: -Wl,-z,max-page-size=16384
```

### A15-BG-NETWORK-LIFECYCLE-EXCEPTION — Network requests outside valid process lifecycle throw exceptions

- **applies_when:** device runs API >= 35 AND app issues network requests not tied to lifecycle
- **failure_mode:** UnknownHostException / IOException at runtime
- **fix:** Wrap network calls in lifecycle-aware components or use WorkManager / foreground service.
- **doc:** https://developer.android.com/about/versions/15/behavior-changes-all#background-network-access-restrictions

> Manual review: Lifecycle-awareness cannot be statically detected; review network code paths.

```
viewLifecycleOwner.lifecycleScope.launch { ... }
```

### A15-MIN-TARGET-SDK-24 — Apps with targetSdkVersion < 24 cannot be installed

- **applies_when:** device runs API >= 35 AND app has targetSdk < 24
- **failure_mode:** INSTALL_FAILED_DEPRECATED_SDK_VERSION
- **fix:** Raise targetSdkVersion to >= 24 (recommended: 35).
- **doc:** https://developer.android.com/about/versions/15/behavior-changes-all#increased-minimum-target-sdk-version-from-23-to-24

**Candidates to review** (1):

- `?` `app/build.gradle:7` — `targetSdkVersion 30          // <-- change this when smoke-testing different rule sets`

```
defaultConfig { targetSdkVersion 35 }
```

### A9-FOREGROUND-SERVICE-PERMISSION — FOREGROUND_SERVICE permission required for any foreground service

- **applies_when:** targetSdk >= 28 AND app uses startForeground()
- **failure_mode:** SecurityException at runtime
- **fix:** Add <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/> to AndroidManifest.xml.
- **doc:** https://developer.android.com/about/versions/pie/android-9.0-changes-28#foreground-services

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/SyncService.kt:26` — `startForeground(1, notification)`

```
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
```

## SILENT

### A10-TLS-SHA1-CERTS-UNTRUSTED — SHA-1 certificates not trusted in TLS

- **applies_when:** device runs API >= 29 AND app pins SHA-1 certs in network_security_config or trusts them via custom TrustManager
- **failure_mode:** TLS handshake fails; connection errors
- **fix:** Replace SHA-1 pins with SHA-256 and ensure server certs use modern hash algorithms.
- **doc:** https://developer.android.com/about/versions/10/behavior-changes-all#certificates-signed-with-sha-1-aren-t-trusted-in-tls

**Findings** (1):

- `!` `app/src/main/res/xml/network_security_config.xml:7` — `<pin digest="SHA-1">deadbeefdeadbeefdeadbeefdeadbeefdeadbeef</pin>`

```
<pin digest="SHA-256">...</pin>
```

### A10-USE-FULL-SCREEN-INTENT-REQUIRED — USE_FULL_SCREEN_INTENT permission required for full-screen-intent notifications

- **applies_when:** targetSdk >= 29 AND code calls setFullScreenIntent()
- **failure_mode:** Full-screen intent is ignored; logcat warns 'Use of fullScreenIntent requires the USE_FULL_SCREEN_INTENT permission'
- **fix:** Add <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>; on A14 also justify per Play policy.
- **doc:** https://developer.android.com/about/versions/10/behavior-changes-10#permissions-changes-for-fullscreen-intents

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:55` — `.setFullScreenIntent(pi, true)`

```
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>
```

### A12-DEPRECATE-DISPLAY-GETREAL — Display.getRealSize / getRealMetrics deprecated (covered above)

- **applies_when:** see A12-DISPLAY-GETREAL-DEPRECATED
- **failure_mode:** Returns constrained bounds
- **fix:** Use WindowManager.getCurrentWindowMetrics().

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:68` — `display.getRealMetrics(metrics)`

```
windowManager.currentWindowMetrics
```

### A12-DISPLAY-GETREAL-DEPRECATED — Display.getRealSize() / getRealMetrics() deprecated and constrained

- **applies_when:** code calls Display.getRealSize / getRealMetrics
- **failure_mode:** Returns constrained bounds for non-resizable apps
- **fix:** Use WindowManager.getCurrentWindowMetrics() / getMaximumWindowMetrics() instead.
- **doc:** https://developer.android.com/about/versions/12/behavior-changes-all#display-getrealsize-and-getrealmetrics-deprecation-and-constraints

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:68` — `display.getRealMetrics(metrics)`

```
windowManager.maximumWindowMetrics.bounds
```

### A12-NOTIFICATION-TRAMPOLINES-BLOCKED — Notification trampolines (start Activity from Service/Receiver) blocked

- **applies_when:** targetSdk >= 31 AND a Service/BroadcastReceiver started from a notification tap calls startActivity()
- **failure_mode:** Activity does not start; logcat warns 'Indirect notification activity start (trampoline)'
- **fix:** Replace trampolines with a PendingIntent directly to the target Activity via Notification.Builder.setContentIntent().
- **doc:** https://developer.android.com/about/versions/12/behavior-changes-12#notification-trampoline-restrictions

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/BootReceiver.kt:11` — `class BootReceiver : BroadcastReceiver() {`

```
builder.setContentIntent(PendingIntent.getActivity(ctx, 0, intent, FLAG_IMMUTABLE))
```

### A13-FGS-NOTIF-PERMISSION-VISIBILITY — Without POST_NOTIFICATIONS, FGS notifications are hidden from drawer

- **applies_when:** targetSdk >= 33 AND app uses foreground services
- **failure_mode:** Users don't see FGS notifications when permission denied
- **fix:** Request POST_NOTIFICATIONS at runtime; gracefully handle denied state.
- **doc:** https://developer.android.com/about/versions/13/behavior-changes-13#notification-permission-affects-foreground-service-appearance

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/SyncService.kt:26` — `startForeground(1, notification)`

```
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

### A13-WEBVIEW-FORCE-DARK-DEPRECATED — WebSettings.setForceDark() is a no-op

- **applies_when:** targetSdk >= 33 AND code calls WebSettings.setForceDark
- **failure_mode:** Dark mode not applied via setForceDark
- **fix:** Use WebSettingsCompat.setAlgorithmicDarkeningAllowed() from AndroidX.
- **doc:** https://developer.android.com/about/versions/13/behavior-changes-13#app-color-theme-applied-automatically-to-webview-content

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:40` — `ws.setForceDark(WebSettings.FORCE_DARK_AUTO)`

```
WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, true)
```

### A14-CACHED-BROADCASTS-QUEUED — Context-registered broadcasts queued while app is cached

- **applies_when:** device runs API >= 34 AND app uses Context.registerReceiver
- **failure_mode:** Broadcasts delivered out of order or merged on resume
- **fix:** Avoid relying on timing of context-registered broadcast delivery; use manifest-declared receivers when ordering matters.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-all#context-registered-broadcasts-are-queued-while-apps-are-cached

> Manual review: Behavioral change at runtime; flag context-registered receivers for review.

```
// move time-sensitive logic out of context-registered receivers
```

### A14-EXACT-ALARM-DENIED-BY-DEFAULT — SCHEDULE_EXACT_ALARM not pre-granted on new installs

- **applies_when:** device runs API >= 34 AND app uses exact alarms
- **failure_mode:** Exact alarms fail until user explicitly grants Special App Access
- **fix:** Check canScheduleExactAlarms() and direct user to settings if denied; consider USE_EXACT_ALARM (calendar/alarm apps).
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-all#schedule-exact-alarms-are-denied-by-default

**Findings** (2):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:46` — `alarm.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60_000, pi)`
- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:47` — `alarm.setAlarmClock(AlarmManager.AlarmClockInfo(0L, pi), pi)`

```
if (!alarmManager.canScheduleExactAlarms()) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
```

### A14-FONT-SCALING-200 — Non-linear font scaling up to 200%

- **applies_when:** device runs API >= 34 AND app uses pixel-based text sizing
- **failure_mode:** UI clipped at high font scale
- **fix:** Use sp units; verify layout under 200% font scale.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-all#non-linear-font-scaling-to-200

> Manual review: Visual review with font scale 200%; no syntactic signature.

```
android:textSize="16sp"
```

### A14-FULL-SCREEN-INTENT-CALLS-ALARMS-ONLY — USE_FULL_SCREEN_INTENT default-revoked except for calling/alarm apps

- **applies_when:** targetSdk >= 34 AND app uses setFullScreenIntent()
- **failure_mode:** Full-screen intent demoted to heads-up; Play revokes default grant for non-calling/alarm apps
- **fix:** Check NotificationManager.canUseFullScreenIntent(); for non-calling/alarm apps, launch ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT to ask the user.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-14#secure-full-screen-intent-notifications

**Candidates to review** (1):

- `?` `app/src/main/java/com/example/brokenapp/MainActivity.kt:55` — `.setFullScreenIntent(pi, true)`

```
if (!nm.canUseFullScreenIntent()) startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT))
```

### A14-KILLBG-OWN-ONLY — ActivityManager.killBackgroundProcesses() limited to own app

- **applies_when:** device runs API >= 34 AND code calls killBackgroundProcesses(otherPackage)
- **failure_mode:** Call is no-op for other apps; only logcat warning
- **fix:** Stop calling killBackgroundProcesses on other packages; let the system manage memory.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-all#apps-can-kill-only-their-own-background-processes

**Candidates to review** (1):

- `?` `app/src/main/java/com/example/brokenapp/MainActivity.kt:59` — `am.killBackgroundProcesses("com.other.package")`

```
// remove killBackgroundProcesses(otherPackageName)
```

### A14-READ-MEDIA-VISUAL-USER-SELECTED — Selected Photos Access (READ_MEDIA_VISUAL_USER_SELECTED)

- **applies_when:** targetSdk >= 34 AND app declares READ_MEDIA_IMAGES/VIDEO with custom gallery picker
- **failure_mode:** App runs in compatibility mode; partial photo grants not handled
- **fix:** Declare READ_MEDIA_VISUAL_USER_SELECTED and handle the selection flow in your gallery picker, or migrate to PhotoPicker.
- **doc:** https://developer.android.com/about/versions/14/behavior-changes-14#partial-access-to-photos-and-videos

**Findings** (1):

- `!` `app/src/main/AndroidManifest.xml:14` — `<!-- Missing: READ_MEDIA_IMAGES/_VIDEO/_AUDIO; only legacy READ_EXTERNAL_STORAGE is declared. -->`

```
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED"/>
```

### A15-DEPRECATE-WEBSQL — WebSettings.setDatabaseEnabled (WebSQL) deprecated

- **applies_when:** code uses WebSettings.setDatabaseEnabled or getDatabaseEnabled
- **failure_mode:** Becomes no-op within 12 months on all Android versions
- **fix:** Migrate web content to IndexedDB or localStorage; remove WebSQL toggles.

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:43` — `ws.setDatabaseEnabled(true)`

```
// remove webSettings.setDatabaseEnabled(true)
```

### A15-ELEGANT-TEXT-HEIGHT-DEFAULT-TRUE — TextView elegantTextHeight defaults to true

- **applies_when:** targetSdk >= 35 AND app supports complex scripts (Arabic/Thai/Indic etc.)
- **failure_mode:** Text layout taller; potential clipping
- **fix:** Test layouts; set elegantTextHeight="false" temporarily if needed (will not be supported in future releases).
- **doc:** https://developer.android.com/about/versions/15/behavior-changes-15#eleganttextheight-attribute-defaults-to-true

> Manual review: Visual change; no syntactic signature.

```
android:elegantTextHeight="false"
```

### A15-STOPPED-STATE-PENDING-INTENTS-CANCELLED — Pending intents cancelled when app enters FLAG_STOPPED

- **applies_when:** device runs API >= 35 AND app registers PendingIntents for widgets / alarms
- **failure_mode:** Widgets greyed out after force-stop; pending intents lost
- **fix:** Re-register pending intents on ACTION_BOOT_COMPLETED; use ApplicationStartInfo.wasForceStopped().
- **doc:** https://developer.android.com/about/versions/15/behavior-changes-all#changes-to-package-stopped-state

**Candidates to review** (1):

- `?` `app/src/main/java/com/example/brokenapp/MainActivity.kt:30` — `val pi = PendingIntent.getActivity(ctx, 0, Intent("X"), 0)`

```
if (ApplicationStartInfo.wasForceStopped()) registerPendingIntents()
```

## DEPRECATION

### A10-ANDROID-BEAM-DEPRECATED — Android Beam (NFC data-sharing) deprecated

- **applies_when:** code uses NfcAdapter.setNdefPushMessage* / setBeamPushUris*
- **failure_mode:** APIs continue to work but are unsupported and disabled on some OEMs
- **fix:** Migrate beam-based NFC sharing to other transports (e.g., Bluetooth, Nearby Share, or direct NFC reader/writer).
- **doc:** https://developer.android.com/about/versions/10/behavior-changes-all#android-beam-deprecation

**Findings** (1):

- `!` `app/src/main/java/com/example/brokenapp/MainActivity.kt:33` — `nfc.setNdefPushMessage(null, null)`

```
// remove NfcAdapter.setNdefPushMessage / setBeamPushUris calls
```

