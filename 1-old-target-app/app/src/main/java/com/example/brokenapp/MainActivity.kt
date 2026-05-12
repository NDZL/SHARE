package com.example.brokenapp

// Deliberately broken activity. Each `// VIOLATES:` comment marks a known-bad call.
// See sample-app/README.md for the rule each line should trigger.

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.view.Display
import android.webkit.WebSettings
import android.widget.Toast

class MainActivity {
    fun things(ctx: Context, am: ActivityManager, alarm: AlarmManager,
               nm: NotificationManager, nfc: NfcAdapter,
               adapter: BluetoothAdapter, ws: WebSettings, display: Display,
               toast: Toast) {

        // VIOLATES: A9-BUILD-SERIAL-UNAVAILABLE
        val serial = Build.SERIAL

        // VIOLATES: A12-PENDING-INTENT-MUTABILITY (no FLAG_IMMUTABLE / FLAG_MUTABLE)
        val pi = PendingIntent.getActivity(ctx, 0, Intent("X"), 0)

        // VIOLATES: A10-ANDROID-BEAM-DEPRECATED
        nfc.setNdefPushMessage(null, null)

        // VIOLATES: A13-BLUETOOTH-ENABLE-DISABLE-DEPRECATED
        adapter.enable()
        adapter.disable()

        // VIOLATES: A13-WEBVIEW-FORCE-DARK-DEPRECATED
        ws.setForceDark(WebSettings.FORCE_DARK_AUTO)

        // VIOLATES: A15-DEPRECATE-WEBSQL
        ws.setDatabaseEnabled(true)

        // VIOLATES: A12-EXACT-ALARM-PERMISSION + A14-EXACT-ALARM-DENIED-BY-DEFAULT
        alarm.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60_000, pi)
        alarm.setAlarmClock(AlarmManager.AlarmClockInfo(0L, pi), pi)

        // VIOLATES: A14-IMPLICIT-INTENTS-EXPLICIT-ONLY (action-string-only Intent constructor)
        ctx.startActivity(Intent("com.example.ACTION_ONLY"))

        // VIOLATES: A10-USE-FULL-SCREEN-INTENT-REQUIRED (no manifest permission)
        val builder = Notification.Builder(ctx, "channel")
            .setContentTitle("ring")
            .setFullScreenIntent(pi, true)
        nm.notify(42, builder.build())

        // VIOLATES: A14-KILLBG-OWN-ONLY
        am.killBackgroundProcesses("com.other.package")

        // NOTE: A11-CUSTOM-TOAST-FROM-BG-BLOCKED would catch Toast.setView(...) calls,
        // but the matching doc heading is H4 (currently outside enumeration's H1-H3 scope).
        // Left here as a reference; the runner won't flag it until the KB is regenerated
        // with deeper enumeration.

        // VIOLATES: A12-DISPLAY-GETREAL-DEPRECATED
        val metrics = android.util.DisplayMetrics()
        display.getRealMetrics(metrics)

        // VIOLATES: A9-INTENT-FLAG-ACTIVITY-NEW-TASK (starting Activity from non-Activity context
        //           without FLAG_ACTIVITY_NEW_TASK)
        ctx.applicationContext.startActivity(Intent(ctx, MainActivity::class.java))
    }
}
