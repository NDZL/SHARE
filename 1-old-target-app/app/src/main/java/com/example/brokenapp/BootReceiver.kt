package com.example.brokenapp

// Deliberately broken BOOT_COMPLETED receiver: it starts a foreground service
// (which Android 15 disallows for dataSync/camera/mediaPlayback/etc.) AND
// also starts an Activity from the receiver (notification-trampoline shape).

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // VIOLATES: A15-BOOT-COMPLETED-FGS-RESTRICTIONS
        context.startForegroundService(Intent(context, SyncService::class.java))

        // VIOLATES: A12-NOTIFICATION-TRAMPOLINES-BLOCKED (Receiver -> startActivity)
        val launch = Intent(context, MainActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
    }
}
