package com.kidshield.kid_shield.services

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kidshield.kid_shield.overlay.BlockingOverlayActivity

/**
 * Source-agnostic app blocking controller.
 *
 * Both the AccessibilityService (instant, optional) and the
 * UsageStatsManager polling engine (primary, always-on) funnel
 * foreground-app detections here. This class owns the shared state
 * (overlay guard, verification sessions, recheck timer) so there
 * is exactly ONE decision-maker regardless of detection source.
 */
class AppBlockingController private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppBlockCtrl"
        private const val PREFS_NAME = "kid_shield_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_PROTECTION_ENABLED = "is_protection_enabled"
        private const val KEY_REVERIFICATION_INTERVAL = "reverification_interval_seconds"
        private const val KEY_VERIFICATION_SESSIONS = "verification_sessions"
        private const val KEY_RECHECK_INTERVAL = "recheck_interval_minutes"

        @Volatile
        private var instance: AppBlockingController? = null

        fun getInstance(context: Context): AppBlockingController {
            return instance ?: synchronized(this) {
                instance ?: AppBlockingController(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())

    // Shared overlay guard
    @Volatile
    var overlayActive = false
        private set
    @Volatile
    var overlayActiveTimestamp = 0L
        private set

    var currentForegroundPackage: String = ""
        private set

    private var recheckRunnable: Runnable? = null

    // Maximum time an overlay can be "active" before we assume it's stuck
    private val OVERLAY_STALE_TIMEOUT_MS = 60_000L

    /**
     * Called by either detection source when a new foreground app is observed.
     * Returns true if the blocking overlay was launched.
     */
    fun onForegroundAppDetected(packageName: String): Boolean {
        // Ignore system UI and our own package
        if (packageName == "com.android.systemui") return false
        if (packageName == "com.kidshield.kid_shield") return false
        if (packageName == context.packageName) return false

        currentForegroundPackage = packageName

        // If overlay is active, check staleness
        if (overlayActive) {
            val elapsed = System.currentTimeMillis() - overlayActiveTimestamp
            if (elapsed > OVERLAY_STALE_TIMEOUT_MS) {
                Log.w(TAG, "overlayActive was stuck for ${elapsed / 1000}s — resetting")
                overlayActive = false
            } else {
                return false // overlay still showing
            }
        }

        // Protection enabled?
        if (!prefs.getBoolean(KEY_PROTECTION_ENABLED, false)) {
            stopRecheckTimer()
            return false
        }

        // Is this app blocked?
        val blockedApps = getBlockedApps()
        if (!blockedApps.contains(packageName)) {
            stopRecheckTimer()
            return false
        }

        // Recently verified?
        if (isRecentlyVerified(packageName)) {
            // Check if daily limit reached — override verification
            val timer = DailyTimerEngine.getInstance(context)
            if (timer.isLimitReached()) {
                Log.d(TAG, "Daily limit reached for $packageName — launching task overlay")
                launchBlockingOverlay(packageName, timeLimitReached = true)
                return true
            }

            Log.d(TAG, "App $packageName was recently verified, allowing access")
            // Start timer tracking for this verified session
            timer.startTracking()
            startRecheckTimer(packageName)
            return false
        }

        Log.d(TAG, "Blocked app detected: $packageName — launching overlay")
        launchBlockingOverlay(packageName)
        return true
    }

    // ─── Overlay lifecycle ────────────────────────────────────────

    private fun launchBlockingOverlay(packageName: String, timeLimitReached: Boolean = false) {
        overlayActive = true
        overlayActiveTimestamp = System.currentTimeMillis()

        // Stop timer tracking while overlay is showing
        DailyTimerEngine.getInstance(context).stopTracking()

        // Log block event for analytics
        try {
            UsageTrackingEngine.getInstance(context).logBlockEvent(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log block event", e)
        }

        val intent = Intent(context, BlockingOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("blocked_package", packageName)
            putExtra("time_limit_reached", timeLimitReached)
        }
        context.startActivity(intent)
    }

    /**
     * Called by BlockingOverlayActivity when overlay is dismissed.
     */
    fun onOverlayDismissed() {
        overlayActive = false
        overlayActiveTimestamp = 0L
        // Stop timer tracking since user left the blocked app (going home)
        DailyTimerEngine.getInstance(context).stopTracking()
        val blockedApps = getBlockedApps()
        if (blockedApps.contains(currentForegroundPackage) &&
            isRecentlyVerified(currentForegroundPackage)
        ) {
            startRecheckTimer(currentForegroundPackage)
        }
    }

    fun resetLastDetected() {
        currentForegroundPackage = ""
        stopRecheckTimer()
    }

    /**
     * Health check called by the foreground service watchdog.
     */
    fun healthCheck() {
        if (overlayActive && overlayActiveTimestamp > 0) {
            val elapsed = System.currentTimeMillis() - overlayActiveTimestamp
            if (elapsed > OVERLAY_STALE_TIMEOUT_MS) {
                Log.w(TAG, "Health check: overlayActive stuck for ${elapsed / 1000}s — resetting")
                overlayActive = false
                overlayActiveTimestamp = 0L
            }
        }
    }

    // ─── Verification helpers ────────────────────────────────────

    fun getBlockedApps(): Set<String> {
        val json = prefs.getString(KEY_BLOCKED_APPS, "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        val list: List<String> = gson.fromJson(json, type)
        return list.toSet()
    }

    fun isRecentlyVerified(packageName: String): Boolean {
        val sessionsJson = prefs.getString(KEY_VERIFICATION_SESSIONS, "{}") ?: "{}"
        val type = object : TypeToken<Map<String, Long>>() {}.type
        val sessions: Map<String, Long> = gson.fromJson(sessionsJson, type)

        val lastVerified = sessions[packageName] ?: return false
        val intervalSeconds = prefs.getInt(KEY_REVERIFICATION_INTERVAL, 1800)

        val intervalMs = if (intervalSeconds == 0) 3000L else intervalSeconds * 1000L
        val elapsed = System.currentTimeMillis() - lastVerified
        return elapsed < intervalMs
    }

    // ─── Recheck timer ────────────────────────────────────────────

    private fun startRecheckTimer(packageName: String) {
        stopRecheckTimer()

        val intervalSeconds = prefs.getInt(KEY_REVERIFICATION_INTERVAL, 1800)
        if (intervalSeconds == 0) return

        val recheckMs = getRecheckIntervalMs()
        Log.d(TAG, "Starting recheck timer: every ${recheckMs / 1000}s for $packageName")

        recheckRunnable = object : Runnable {
            override fun run() {
                if (currentForegroundPackage != packageName) {
                    Log.d(TAG, "Recheck: user left $packageName, stopping timer")
                    stopRecheckTimer()
                    return
                }
                if (overlayActive) {
                    handler.postDelayed(this, recheckMs)
                    return
                }
                if (!isRecentlyVerified(packageName)) {
                    Log.d(TAG, "Recheck: verification expired for $packageName")
                    launchBlockingOverlay(packageName)
                } else {
                    handler.postDelayed(this, recheckMs)
                }
            }
        }
        handler.postDelayed(recheckRunnable!!, recheckMs)
    }

    private fun stopRecheckTimer() {
        recheckRunnable?.let { handler.removeCallbacks(it) }
        recheckRunnable = null
    }

    private fun getRecheckIntervalMs(): Long {
        val recheckMinutes = prefs.getInt(KEY_RECHECK_INTERVAL, 1)
        return (recheckMinutes * 60 * 1000L).coerceAtLeast(30_000L)
    }
}
