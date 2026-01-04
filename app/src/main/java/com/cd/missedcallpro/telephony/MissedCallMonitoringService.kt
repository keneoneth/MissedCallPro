package com.cd.missedcallpro.telephony

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cd.missedcallpro.R

class MissedCallMonitoringService : Service() {

    companion object {
        private const val CHANNEL_ID = "mc_monitoring"
        private const val NOTIF_ID = 3001
    }

    private var receiver: PhoneStateReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()

        receiver = PhoneStateReceiver()
        val filter = IntentFilter().apply {
            addAction("android.intent.action.PHONE_STATE")
        }
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        // Don’t auto-restart if system kills it; aligns with “killed=no monitoring”
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // User swiped app away from Recents => stop monitoring
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        receiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        receiver = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification() = run {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPI = android.app.PendingIntent.getActivity(
            this, 0, launchIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // temporary to rule out icon issues
            .setContentTitle("Missed call monitoring")
            .setContentText("Running in background. Tap to open app.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentPI)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Missed call monitoring",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(ch)
        }
    }
}
