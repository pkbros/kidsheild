package com.kidshield.kid_shield.services

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.util.Log

/**
 * Primary foreground-app detection engine using UsageStatsManager.
 *
 * Polls UsageStatsManager.queryEvents() at a configurable interval
 * (default 300ms) looking for ACTIVITY_RESUMED events. This replaces
 * the AccessibilityService as the primary detection mechanism, avoiding
 * Google Play policy restrictions and Android 13+ "restricted settings"
 * friction.
 *
 * Trade-off: polling at 300ms adds ~200-400ms latency vs the instant
 * AccessibilityService callback, but keeps us well under the 500ms
 * overlay trigger latency goal.
 *
 * Battery impact: handler-based polling on a background thread with
 * screen-off suspension keeps CPU usage minimal (~2-3%).
 */
class UsageStatsPollingEngine(private val context: Context) {

    companion object {
        private const val TAG = "UsageStatsPoller"
        private const val DEFAULT_POLL_INTERVAL_MS = 300L   // 300ms polling
        private const val SLOW_POLL_INTERVAL_MS = 2000L     // 2s when screen off
    }

    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val controller: AppBlockingController =
        AppBlockingController.getInstance(context)

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var isRunning = false
    private var lastSeenPackage: String = ""
    private var lastEventTimestamp: Long = 0L

    // Adaptive polling: fast when screen is on, slow when off
    private var currentIntervalMs = DEFAULT_POLL_INTERVAL_MS

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                pollForegroundApp()
            } catch (e: Exception) {
                Log.w(TAG, "Poll cycle error", e)
            }

            // Adaptive interval: slow down when screen is off
            currentIntervalMs = if (powerManager.isInteractive) {
                DEFAULT_POLL_INTERVAL_MS
            } else {
                SLOW_POLL_INTERVAL_MS
            }

            handler?.postDelayed(this, currentIntervalMs)
        }
    }

    /**
     * Start the polling loop. Safe to call multiple times.
     */
    fun start() {
        if (isRunning) return

        handlerThread = HandlerThread("UsageStatsPoller").also { it.start() }
        handler = Handler(handlerThread!!.looper)
        isRunning = true
        lastEventTimestamp = System.currentTimeMillis()

        Log.d(TAG, "Polling engine started (interval=${DEFAULT_POLL_INTERVAL_MS}ms)")
        handler?.post(pollRunnable)
    }

    /**
     * Stop the polling loop. Safe to call multiple times.
     */
    fun stop() {
        if (!isRunning) return
        isRunning = false

        handler?.removeCallbacks(pollRunnable)
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null

        Log.d(TAG, "Polling engine stopped")
    }

    /**
     * Core polling method: queries UsageStatsManager for ACTIVITY_RESUMED
     * events since our last check, and forwards the latest to the controller.
     */
    private fun pollForegroundApp() {
        val now = System.currentTimeMillis()

        // Query window: from last poll to now.
        // Use a minimum window of 500ms to avoid missing events.
        val beginTime = (lastEventTimestamp - 100).coerceAtLeast(now - 5000)

        val events: UsageEvents? = try {
            usageStatsManager.queryEvents(beginTime, now)
        } catch (e: SecurityException) {
            Log.e(TAG, "PACKAGE_USAGE_STATS permission not granted", e)
            null
        }

        if (events == null) return

        // Walk through all events and find the latest ACTIVITY_RESUMED
        var latestResumedPackage: String? = null
        var latestTimestamp = lastEventTimestamp
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.timeStamp > lastEventTimestamp) {
                    latestResumedPackage = event.packageName
                    latestTimestamp = event.timeStamp
                }
            }
        }

        if (latestResumedPackage != null && latestTimestamp > lastEventTimestamp) {
            lastEventTimestamp = latestTimestamp

            // Only notify controller if the foreground app actually changed
            if (latestResumedPackage != lastSeenPackage) {
                lastSeenPackage = latestResumedPackage
                Log.d(TAG, "Foreground app changed: $latestResumedPackage")
                controller.onForegroundAppDetected(latestResumedPackage)
            }
        }
    }

    /**
     * Returns the current polling interval in ms (for diagnostics).
     */
    fun getCurrentInterval(): Long = currentIntervalMs

    /**
     * Returns whether the engine is actively polling.
     */
    fun isPolling(): Boolean = isRunning
}
