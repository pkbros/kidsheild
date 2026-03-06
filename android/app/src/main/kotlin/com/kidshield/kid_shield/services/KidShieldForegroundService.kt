package com.kidshield.kid_shield.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kidshield.kid_shield.MainActivity
import com.kidshield.kid_shield.R

class KidShieldForegroundService : Service() {

    companion object {
        private const val TAG = "KidShieldFGService"
        private const val CHANNEL_ID = "kidshield_monitoring"
        private const val NOTIFICATION_ID = 1001
        private const val WATCHDOG_INTERVAL_MS = 30_000L // 30 seconds
        var isRunning = false
            private set
    }

    private var pollingEngine: UsageStatsPollingEngine? = null

    private val watchdogHandler = Handler(Looper.getMainLooper())
    private val watchdogRunnable = object : Runnable {
        override fun run() {
            // Health-check the blocking controller (replaces old a11y healthCheck)
            AppBlockingController.getInstance(this@KidShieldForegroundService).healthCheck()

            // Also ping the accessibility service if it's active (optional accelerator)
            KidShieldAccessibilityService.instance?.healthCheck()

            // Check streak milestones and update screen time (analytics)
            try {
                val tracker = UsageTrackingEngine.getInstance(this@KidShieldForegroundService)
                tracker.checkStreakMilestones()
                tracker.updateScreenTimeRecords()
            } catch (e: Exception) {
                Log.w(TAG, "Analytics watchdog tick failed", e)
            }

            watchdogHandler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Foreground service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Foreground service started")
        isRunning = true

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start the UsageStatsManager polling engine (primary detection)
        if (hasUsageStatsPermission()) {
            pollingEngine = UsageStatsPollingEngine(this).also { it.start() }
            Log.d(TAG, "UsageStats polling engine started (primary detection)")
        } else {
            Log.w(TAG, "UsageStats permission not granted — relying on AccessibilityService only")
        }

        // Start watchdog
        watchdogHandler.removeCallbacks(watchdogRunnable)
        watchdogHandler.postDelayed(watchdogRunnable, WATCHDOG_INTERVAL_MS)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        pollingEngine?.stop()
        pollingEngine = null
        watchdogHandler.removeCallbacks(watchdogRunnable)
        Log.d(TAG, "Foreground service destroyed")
        super.onDestroy()
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /**
     * Returns whether the polling engine is currently active.
     */
    fun isPollingActive(): Boolean = pollingEngine?.isPolling() == true

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Task removed — scheduling restart")
        // Restart service if task is removed
        val restartIntent = Intent(applicationContext, KidShieldForegroundService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.setExact(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "KidShield Protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors app usage to protect your child"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KidShield Active")
            .setContentText("Protection is running")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
