package com.kidshield.kid_shield.services

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tracks how many minutes the child has used blocked apps today
 * and determines whether the daily limit has been reached.
 *
 * Flow:
 * 1. Parent sets a daily limit (default: 30 min)
 * 2. Each time a blocked app is verified and accessed, the timer starts
 * 3. When cumulative usage = limit → overlay blocks with task-earn prompt
 * 4. Completing a task (parent-verified) adds bonus minutes
 * 5. First open of the day shows a "use your time wisely" overlay
 */
class DailyTimerEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DailyTimer"
        private const val PREFS_NAME = "kid_shield_timer"
        private const val KEY_DAILY_LIMIT = "daily_limit_minutes"
        private const val KEY_USED_TODAY = "used_today_seconds"
        private const val KEY_BONUS_TODAY = "bonus_today_minutes"
        private const val KEY_LAST_RESET_DATE = "timer_last_reset_date"
        private const val KEY_FIRST_OPEN_SHOWN = "first_open_shown_date"
        private const val KEY_TIMER_ENABLED = "daily_timer_enabled"
        private const val TICK_INTERVAL_MS = 10_000L  // 10 seconds

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fun todayString(): String = dateFormat.format(Date())

        @Volatile
        private var instance: DailyTimerEngine? = null

        fun getInstance(context: Context): DailyTimerEngine {
            return instance ?: synchronized(this) {
                instance ?: DailyTimerEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var isTracking = false
    private var trackingStartedAt = 0L

    private var tickRunnable: Runnable? = null

    init {
        resetIfNewDay()
    }

    // ─── Configuration ────────────────────────────────────

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_TIMER_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TIMER_ENABLED, enabled).apply()
        if (!enabled) stopTracking()
    }

    fun getDailyLimitMinutes(): Int = prefs.getInt(KEY_DAILY_LIMIT, 30)

    fun setDailyLimitMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_LIMIT, minutes.coerceIn(5, 480)).apply()
    }

    // ─── Daily Reset ──────────────────────────────────────

    private fun resetIfNewDay() {
        val lastReset = prefs.getString(KEY_LAST_RESET_DATE, "") ?: ""
        val today = todayString()
        if (lastReset != today) {
            Log.d(TAG, "New day ($lastReset → $today): resetting daily timer")
            prefs.edit()
                .putInt(KEY_USED_TODAY, 0)
                .putInt(KEY_BONUS_TODAY, 0)
                .putString(KEY_LAST_RESET_DATE, today)
                .apply()
        }
    }

    // ─── Time Tracking ────────────────────────────────────

    /**
     * Start tracking active usage of a verified blocked app.
     * Called when child is verified and allowed into a blocked app.
     */
    fun startTracking() {
        if (!isEnabled()) return
        if (isTracking) return
        resetIfNewDay()

        isTracking = true
        trackingStartedAt = System.currentTimeMillis()
        startTicker()
        Log.d(TAG, "Started tracking usage")
    }

    /**
     * Stop tracking usage (child left the blocked app).
     */
    fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        // Flush elapsed time
        val elapsed = System.currentTimeMillis() - trackingStartedAt
        if (elapsed > 0) {
            addUsedSeconds((elapsed / 1000).toInt())
        }
        stopTicker()
        Log.d(TAG, "Stopped tracking. Total used: ${getUsedTodayMinutes()} min")
    }

    private fun startTicker() {
        stopTicker()
        tickRunnable = object : Runnable {
            override fun run() {
                if (!isTracking) return
                // Flush elapsed time periodically
                val now = System.currentTimeMillis()
                val elapsed = now - trackingStartedAt
                if (elapsed > 0) {
                    addUsedSeconds((elapsed / 1000).toInt())
                    trackingStartedAt = now
                }
                handler.postDelayed(this, TICK_INTERVAL_MS)
            }
        }
        handler.postDelayed(tickRunnable!!, TICK_INTERVAL_MS)
    }

    private fun stopTicker() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
    }

    private fun addUsedSeconds(seconds: Int) {
        val current = prefs.getInt(KEY_USED_TODAY, 0)
        prefs.edit().putInt(KEY_USED_TODAY, current + seconds).apply()
    }

    // ─── Queries ──────────────────────────────────────────

    /**
     * Total seconds of blocked app usage today.
     */
    fun getUsedTodaySeconds(): Int {
        resetIfNewDay()
        var total = prefs.getInt(KEY_USED_TODAY, 0)
        // Add in-flight time if currently tracking
        if (isTracking) {
            total += ((System.currentTimeMillis() - trackingStartedAt) / 1000).toInt()
        }
        return total
    }

    fun getUsedTodayMinutes(): Int = getUsedTodaySeconds() / 60

    /**
     * Bonus minutes earned today from completed tasks.
     */
    fun getBonusTodayMinutes(): Int {
        resetIfNewDay()
        return prefs.getInt(KEY_BONUS_TODAY, 0)
    }

    /**
     * Add bonus minutes (called when a task is completed).
     */
    fun addBonusMinutes(minutes: Int) {
        resetIfNewDay()
        val current = prefs.getInt(KEY_BONUS_TODAY, 0)
        prefs.edit().putInt(KEY_BONUS_TODAY, current + minutes).apply()
        Log.d(TAG, "Added $minutes bonus minutes (total bonus: ${current + minutes})")
    }

    /**
     * Effective daily limit = base limit + bonus from tasks.
     */
    fun getEffectiveLimitMinutes(): Int {
        return getDailyLimitMinutes() + getBonusTodayMinutes()
    }

    /**
     * Remaining minutes the child can use blocked apps today.
     */
    fun getRemainingMinutes(): Int {
        val remaining = getEffectiveLimitMinutes() - getUsedTodayMinutes()
        return remaining.coerceAtLeast(0)
    }

    /**
     * Whether the daily limit has been reached.
     */
    fun isLimitReached(): Boolean {
        if (!isEnabled()) return false
        return getUsedTodayMinutes() >= getEffectiveLimitMinutes()
    }

    // ─── First Open of the Day ────────────────────────────

    /**
     * Whether the "use wisely" video has been shown today.
     */
    fun hasShownFirstOpenToday(): Boolean {
        val shown = prefs.getString(KEY_FIRST_OPEN_SHOWN, "") ?: ""
        return shown == todayString()
    }

    /**
     * Mark the first-open video as shown for today.
     */
    fun markFirstOpenShown() {
        prefs.edit().putString(KEY_FIRST_OPEN_SHOWN, todayString()).apply()
    }

    // ─── Status Summary ───────────────────────────────────

    /**
     * Get a full status map for Flutter UI consumption.
     */
    fun getStatus(): Map<String, Any> {
        resetIfNewDay()
        return mapOf(
            "enabled" to isEnabled(),
            "dailyLimitMinutes" to getDailyLimitMinutes(),
            "usedTodayMinutes" to getUsedTodayMinutes(),
            "bonusTodayMinutes" to getBonusTodayMinutes(),
            "effectiveLimitMinutes" to getEffectiveLimitMinutes(),
            "remainingMinutes" to getRemainingMinutes(),
            "limitReached" to isLimitReached(),
            "firstOpenShownToday" to hasShownFirstOpenToday()
        )
    }
}
