package com.example.brokenapp

// Deliberately broken service. Each `// VIOLATES:` comment marks a known-bad call.

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder

class SyncService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = Notification.Builder(this, "ch")
            .setContentTitle("syncing")
            .build()

        // VIOLATES: A9-FOREGROUND-SERVICE-PERMISSION (no FOREGROUND_SERVICE in manifest)
        // VIOLATES: A14-FGS-TYPES-REQUIRED (no foregroundServiceType on <service>)
        startForeground(1, notification)

        // VIOLATES: A14-BLUETOOTH-CONNECT-ENFORCED (no BLUETOOTH_CONNECT)
        BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(0)

        // VIOLATES: A14-RECEIVER-EXPORTED-FLAG (no RECEIVER_EXPORTED / RECEIVER_NOT_EXPORTED)
        val recv = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { /* ... */ }
        }
        registerReceiver(recv, IntentFilter("com.example.brokenapp.PING"))

        // VIOLATES: A13-POST-NOTIFICATIONS-PERMISSION (no POST_NOTIFICATIONS declared)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(2, notification)

        return START_STICKY
    }
}
