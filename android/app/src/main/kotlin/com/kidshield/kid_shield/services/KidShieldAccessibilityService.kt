package com.kidshield.kid_shield.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kidshield.kid_shield.overlay.BlockingOverlayActivity

class KidShieldAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KidShieldA11y"
        private const val PREFS_NAME = "kid_shield_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_PROTECTION_ENABLED = "is_protection_enabled"
        private const val KEY_REVERIFICATION_INTERVAL = "reverification_interval_seconds"
        private const val KEY_VERIFICATION_SESSIONS = "verification_sessions"
        private const val KEY_RECHECK_INTERVAL = "recheck_interval_minutes"
        var instance: KidShieldAccessibilityService? = null
            private set
    }

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    private var currentForegroundPackage: String = ""
    private var overlayActive = false
    private var overlayActiveTimestamp: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var recheckRunnable: Runnable? = null

    // Maximum time an overlay can be "active" before we assume it's stuck
    private val OVERLAY_STALE_TIMEOUT_MS = 60_000L

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        Log.d(TAG, "Accessibility Service created")
    }

    override fun onDestroy() {
        instance = null
        stopRecheckTimer()
        super.onDestroy()
        Log.d(TAG, "Accessibility Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore system UI and our own overlay
        if (packageName == "com.android.systemui") return
        if (packageName == "com.kidshield.kid_shield") return
        if (packageName == this.packageName) return

        // Track current foreground app
        currentForegroundPackage = packageName

        // If overlay is currently showing, don't stack another
        // But check for staleness — if overlayActive has been true for too long,
        // the activity was likely killed without calling onOverlayDismissed()
        if (overlayActive) {
            val elapsed = System.currentTimeMillis() - overlayActiveTimestamp
            if (elapsed > OVERLAY_STALE_TIMEOUT_MS) {
                Log.w(TAG, "overlayActive was stuck for ${elapsed / 1000}s — resetting")
                overlayActive = false
                // Fall through to re-evaluate
            } else {
                return
            }
        }

        // Check if protection is enabled
        if (!prefs.getBoolean(KEY_PROTECTION_ENABLED, false)) {
            stopRecheckTimer()
            return
        }

        // Check if this app is blocked
        val blockedApps = getBlockedApps()
        if (!blockedApps.contains(packageName)) {
            stopRecheckTimer()
            return
        }

        // Check if recently verified
        if (isRecentlyVerified(packageName)) {
            Log.d(TAG, "App $packageName was recently verified, allowing access")
            // Start recheck timer so we catch when verification expires
            startRecheckTimer(packageName)
            return
        }

        Log.d(TAG, "Blocked app detected: $packageName - launching overlay")
        launchBlockingOverlay(packageName)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected")
    }

    private fun getBlockedApps(): Set<String> {
        val json = prefs.getString(KEY_BLOCKED_APPS, "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        val list: List<String> = gson.fromJson(json, type)
        return list.toSet()
    }

    private fun isRecentlyVerified(packageName: String): Boolean {
        val sessionsJson = prefs.getString(KEY_VERIFICATION_SESSIONS, "{}") ?: "{}"
        val type = object : TypeToken<Map<String, Long>>() {}.type
        val sessions: Map<String, Long> = gson.fromJson(sessionsJson, type)

        val lastVerified = sessions[packageName] ?: return false
        val intervalSeconds = prefs.getInt(KEY_REVERIFICATION_INTERVAL, 1800)

        // "Every time" mode (0) still grants a 3-second grace period so the
        // overlay doesn't immediately re-trigger when the blocked app regains
        // foreground focus after successful verification.
        val intervalMs = if (intervalSeconds == 0) {
            3000L
        } else {
            intervalSeconds * 1000L
        }

        val elapsed = System.currentTimeMillis() - lastVerified
        return elapsed < intervalMs
    }

    /**
     * Starts a periodic timer that re-checks if verification has expired
     * while the user continues using a blocked app. When it expires the
     * overlay is launched again without the user needing to switch apps.
     */
    private fun startRecheckTimer(packageName: String) {
        stopRecheckTimer()

        val intervalSeconds = prefs.getInt(KEY_REVERIFICATION_INTERVAL, 1800)
        if (intervalSeconds == 0) return  // "every time" mode, no periodic re-check

        val recheckMs = getRecheckIntervalMs()
        Log.d(TAG, "Starting recheck timer: every ${recheckMs / 1000}s for $packageName")

        recheckRunnable = object : Runnable {
            override fun run() {
                // If user left the blocked app, stop this timer
                if (currentForegroundPackage != packageName) {
                    Log.d(TAG, "Recheck: user left $packageName, stopping timer")
                    // Don't return silently — explicitly stop so we don't leak
                    stopRecheckTimer()
                    return
                }
                if (overlayActive) {
                    // Overlay is showing, keep polling but don't launch another
                    handler.postDelayed(this, recheckMs)
                    return
                }
                if (!isRecentlyVerified(packageName)) {
                    Log.d(TAG, "Recheck: verification expired for $packageName")
                    launchBlockingOverlay(packageName)
                    // Don't re-post: overlay is now active. onOverlayDismissed() will restart timer if needed.
                } else {
                    // Still verified, keep checking
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

    /**
     * Returns the periodic re-check interval in milliseconds.
     * Defaults to 1 minute. Minimum 30 seconds.
     */
    private fun getRecheckIntervalMs(): Long {
        val recheckMinutes = prefs.getInt(KEY_RECHECK_INTERVAL, 1)
        return (recheckMinutes * 60 * 1000L).coerceAtLeast(30_000L)
    }

    private fun launchBlockingOverlay(packageName: String) {
        overlayActive = true
        overlayActiveTimestamp = System.currentTimeMillis()

        // Log block event in usage analytics
        try {
            UsageTrackingEngine.getInstance(this).logBlockEvent(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log block event", e)
        }

        val intent = Intent(this, BlockingOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("blocked_package", packageName)
        }
        startActivity(intent)
    }

    /**
     * Called by BlockingOverlayActivity when overlay is dismissed
     * (either via successful verification or going home).
     */
    fun onOverlayDismissed() {
        overlayActive = false
        overlayActiveTimestamp = 0L
        // If the blocked app is still in foreground and now verified,
        // start the recheck timer to catch when verification expires.
        val blockedApps = getBlockedApps()
        if (blockedApps.contains(currentForegroundPackage) &&
            isRecentlyVerified(currentForegroundPackage)) {
            startRecheckTimer(currentForegroundPackage)
        }
    }

    fun resetLastDetected() {
        currentForegroundPackage = ""
        stopRecheckTimer()
    }

    /**
     * Health check called by the foreground service watchdog.
     * Resets overlayActive if it has been stuck too long.
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
}
