package com.kidshield.kid_shield.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kidshield.kid_shield.MainActivity
import java.util.Calendar
import java.util.concurrent.Executors

/**
 * Central engine for usage analytics:
 *  - Block event logging (with outcome tracking)
 *  - Screen-free streak tracking
 *  - Screen time queries via UsageStatsManager
 *  - Daily aggregation
 *  - Streak milestone & parent nudge notifications
 */
class UsageTrackingEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "UsageTracking"
        private const val PREFS_NAME = "kid_shield_prefs"
        private const val KEY_CURRENT_STREAK_ID = "current_streak_id"
        private const val KEY_PARENT_TRACKING_ENABLED = "parent_tracking_enabled"
        private const val KEY_PARENT_SCREEN_TIME_GOAL = "parent_screen_time_goal_minutes"
        private const val KEY_NUDGE_THRESHOLD = "nudge_threshold"
        private const val KEY_LAST_AGGREGATE_DATE = "last_aggregate_date"
        private const val KEY_LAST_NUDGE_TIMESTAMP = "last_nudge_timestamp"
        private const val KEY_LAST_STREAK_MILESTONE = "last_streak_milestone_minutes"
        private const val NUDGE_COOLDOWN_MS = 3_600_000L // 1 hour between nudge notifications
        private const val STREAK_CHANNEL_ID = "kidshield_streaks"
        private const val NUDGE_CHANNEL_ID = "kidshield_nudges"
        private const val STREAK_NOTIFICATION_ID = 2001
        private const val NUDGE_NOTIFICATION_ID = 2002

        // Streak milestones (in minutes)
        private val STREAK_MILESTONES = listOf(60, 120, 240, 480) // 1h, 2h, 4h, 8h (all-day proxy)

        @Volatile
        private var instance: UsageTrackingEngine? = null

        fun getInstance(context: Context): UsageTrackingEngine {
            return instance ?: synchronized(this) {
                instance ?: UsageTrackingEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val db = UsageDatabase.getInstance(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    // In-memory cache of the current block event ID (set when overlay launches)
    @Volatile
    var currentBlockEventId: Long = -1L
        private set

    @Volatile
    var currentBlockedPackage: String = ""
        private set

    init {
        createNotificationChannels()
        // Ensure a streak is active on startup
        ensureActiveStreak()
        // Run daily aggregation for yesterday if not done
        executor.execute { runDailyAggregationIfNeeded() }
    }

    // ===================================================================
    // Block Event Tracking
    // ===================================================================

    /**
     * Log a block event when the overlay is triggered.
     * Called from AccessibilityService.launchBlockingOverlay().
     */
    fun logBlockEvent(appPackage: String) {
        executor.execute {
            val id = db.insertBlockEvent(appPackage)
            currentBlockEventId = id
            currentBlockedPackage = appPackage
            Log.d(TAG, "Logged block event #$id for $appPackage")

            // End current streak (blocked app was opened)
            endCurrentStreak()

            // Check nudge threshold
            checkNudgeThreshold(appPackage)
        }
    }

    /**
     * Update the outcome of the current block event.
     * Called when verification succeeds or user navigates away.
     */
    fun recordBlockOutcome(outcome: String) {
        val eventId = currentBlockEventId
        if (eventId > 0) {
            executor.execute {
                db.updateBlockEventOutcome(eventId, outcome)
                Log.d(TAG, "Block event #$eventId outcome: $outcome")
            }
        }
    }

    /**
     * Called when the overlay is dismissed. Starts a new streak.
     */
    fun onOverlayDismissed() {
        executor.execute {
            startNewStreak()
        }
    }

    // ===================================================================
    // Screen-Free Streak Tracking
    // ===================================================================

    private fun ensureActiveStreak() {
        executor.execute {
            val active = db.getActiveStreak()
            if (active == null) {
                val id = db.startStreak()
                prefs.edit().putLong(KEY_CURRENT_STREAK_ID, id).apply()
            } else {
                prefs.edit().putLong(KEY_CURRENT_STREAK_ID, active["id"] as Long).apply()
            }
        }
    }

    private fun endCurrentStreak() {
        val streakId = prefs.getLong(KEY_CURRENT_STREAK_ID, -1L)
        if (streakId > 0) {
            db.endStreak(streakId)
            prefs.edit().putLong(KEY_CURRENT_STREAK_ID, -1L).apply()
        }
    }

    private fun startNewStreak() {
        val id = db.startStreak()
        prefs.edit()
            .putLong(KEY_CURRENT_STREAK_ID, id)
            .putInt(KEY_LAST_STREAK_MILESTONE, 0) // reset milestone tracker
            .apply()
        Log.d(TAG, "New streak started: id=$id")
    }

    /**
     * Check current streak duration and send milestone notifications.
     * Called periodically from the foreground service watchdog.
     */
    fun checkStreakMilestones() {
        executor.execute {
            val active = db.getActiveStreak() ?: return@execute
            val durationMinutes = active["duration_minutes"] as Int
            val lastMilestone = prefs.getInt(KEY_LAST_STREAK_MILESTONE, 0)

            for (milestone in STREAK_MILESTONES) {
                if (durationMinutes >= milestone && lastMilestone < milestone) {
                    sendStreakNotification(milestone, durationMinutes)
                    prefs.edit().putInt(KEY_LAST_STREAK_MILESTONE, milestone).apply()
                    break // one notification at a time
                }
            }
        }
    }

    /**
     * Get current streak info for the dashboard.
     */
    fun getCurrentStreakInfo(): Map<String, Any> {
        val active = db.getActiveStreak()
        val todayBest = db.getTodayBestStreak()
        val longestEver = db.getLongestStreakEver()

        return mapOf(
            "current_minutes" to (active?.get("duration_minutes") as? Int ?: 0),
            "is_active" to (active != null),
            "today_best_minutes" to todayBest,
            "longest_ever_minutes" to longestEver
        )
    }

    // ===================================================================
    // Screen Time (via UsageStatsManager)
    // ===================================================================

    /**
     * Query device screen time for today using UsageStatsManager.
     * Returns total screen time in minutes.
     */
    fun queryChildScreenTime(): Int {
        return queryScreenTimeSince(getStartOfDayMs())
    }

    /**
     * Query total device usage time since a timestamp.
     * This counts foreground time of all user-facing apps.
     */
    private fun queryScreenTimeSince(sinceMs: Long): Int {
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, sinceMs, now)
            if (stats.isNullOrEmpty()) return 0

            var totalMs = 0L
            for (stat in stats) {
                // Skip system packages
                if (stat.packageName.startsWith("com.android.systemui")) continue
                if (stat.packageName == context.packageName) continue
                totalMs += stat.totalTimeInForeground
            }
            return (totalMs / 60_000).toInt()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query screen time", e)
            return 0
        }
    }

    /**
     * Query parent's screen time (total device usage for today).
     * Same as child screen time — the distinction is conceptual.
     * Parent screen time is device-level usage while KidShield is running.
     */
    fun queryParentScreenTime(): Int {
        if (!isParentTrackingEnabled()) return 0
        return queryChildScreenTime() // Same API; parent uses same device metrics
    }

    /**
     * Persist today's screen time into the database.
     * Called periodically from the foreground service.
     */
    fun updateScreenTimeRecords() {
        executor.execute {
            val today = UsageDatabase.todayString()
            val childMinutes = queryChildScreenTime()
            db.setScreenTime(today, "child", childMinutes)

            if (isParentTrackingEnabled()) {
                val parentMinutes = queryParentScreenTime()
                db.setScreenTime(today, "parent", parentMinutes)
            }
        }
    }

    // ===================================================================
    // Daily Aggregation
    // ===================================================================

    /**
     * Run daily aggregation for any dates that haven't been aggregated yet.
     */
    private fun runDailyAggregationIfNeeded() {
        val today = UsageDatabase.todayString()
        val lastAggDate = prefs.getString(KEY_LAST_AGGREGATE_DATE, null)

        if (lastAggDate != null && lastAggDate != today) {
            // Aggregate yesterday (and any missed days)
            db.computeDailyAggregate(lastAggDate)

            // Also aggregate today (partial, for dashboard)
            db.computeDailyAggregate(today)
        } else if (lastAggDate == null) {
            // First run — aggregate today
            db.computeDailyAggregate(today)
        }

        prefs.edit().putString(KEY_LAST_AGGREGATE_DATE, today).apply()

        // Prune old data
        db.pruneOldData()
    }

    /**
     * Force refresh today's aggregate (called from dashboard).
     */
    fun refreshTodayAggregate() {
        executor.execute {
            updateScreenTimeRecords()
            val today = UsageDatabase.todayString()
            db.computeDailyAggregate(today)
        }
    }

    // ===================================================================
    // Dashboard Data
    // ===================================================================

    /**
     * Get today's stats for the dashboard.
     */
    fun getTodayStats(): Map<String, Any?> {
        // Refresh first
        val today = UsageDatabase.todayString()
        updateScreenTimeRecords()
        db.computeDailyAggregate(today)

        val agg = db.getDailyAggregate(today)
        val streak = getCurrentStreakInfo()
        val topApps = db.getTopBlockedApps(today, today, 5)

        return mapOf(
            "date" to today,
            "total_block_attempts" to (agg?.get("total_block_attempts") ?: 0),
            "total_verified" to (agg?.get("total_verified") ?: 0),
            "total_screen_time_minutes" to (agg?.get("total_screen_time_minutes") ?: 0),
            "parent_screen_time_minutes" to agg?.get("parent_screen_time_minutes"),
            "best_streak_minutes" to (agg?.get("best_streak_minutes") ?: 0),
            "most_attempted_app" to agg?.get("most_attempted_app"),
            "current_streak" to streak,
            "top_blocked_apps" to topApps
        )
    }

    /**
     * Get yesterday's stats for trend comparison.
     */
    fun getYesterdayStats(): Map<String, Any?>? {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = UsageDatabase.dateString(cal.timeInMillis)
        return db.getDailyAggregate(yesterday)
    }

    /**
     * Get weekly summary (last 7 days of daily aggregates).
     */
    fun getWeeklySummary(): List<Map<String, Any?>> {
        val cal = Calendar.getInstance()
        val endDate = UsageDatabase.todayString()

        cal.add(Calendar.DAY_OF_YEAR, -6) // 7 days including today
        val startDate = UsageDatabase.dateString(cal.timeInMillis)

        return db.getDailyAggregates(startDate, endDate)
    }

    /**
     * Get block event history for a specific date.
     */
    fun getBlockEventsForDate(date: String): List<Map<String, Any>> {
        return db.getBlockEventsForDate(date)
    }

    // ===================================================================
    // Parent Self-Awareness
    // ===================================================================

    fun isParentTrackingEnabled(): Boolean {
        return prefs.getBoolean(KEY_PARENT_TRACKING_ENABLED, false)
    }

    fun setParentTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PARENT_TRACKING_ENABLED, enabled).apply()
    }

    fun getParentScreenTimeGoal(): Int {
        return prefs.getInt(KEY_PARENT_SCREEN_TIME_GOAL, 0) // 0 = no goal set
    }

    fun setParentScreenTimeGoal(minutes: Int) {
        prefs.edit().putInt(KEY_PARENT_SCREEN_TIME_GOAL, minutes).apply()
    }

    /**
     * Generate a contextual parent insight message.
     */
    fun getParentInsight(): String? {
        if (!isParentTrackingEnabled()) return null
        val parentMinutes = queryParentScreenTime()

        return when {
            parentMinutes >= 300 -> "You've been on for ${parentMinutes / 60}+ hours today. Kids learn by watching!"
            parentMinutes >= 240 -> "Over 4 hours of screen time today. Maybe take a break together?"
            parentMinutes >= 180 -> "3+ hours on screen today. How about some family activity time?"
            parentMinutes >= 120 -> "2 hours of screen time logged. You're doing great being mindful!"
            else -> null
        }
    }

    // ===================================================================
    // Nudge Notifications
    // ===================================================================

    fun getNudgeThreshold(): Int {
        return prefs.getInt(KEY_NUDGE_THRESHOLD, 5) // default: 5 attempts per hour
    }

    fun setNudgeThreshold(threshold: Int) {
        prefs.edit().putInt(KEY_NUDGE_THRESHOLD, threshold).apply()
    }

    /**
     * Check if the child has exceeded the nudge threshold for a specific app in the last hour.
     */
    private fun checkNudgeThreshold(appPackage: String) {
        val threshold = getNudgeThreshold()
        if (threshold <= 0) return // nudges disabled

        val lastNudge = prefs.getLong(KEY_LAST_NUDGE_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        if (now - lastNudge < NUDGE_COOLDOWN_MS) return // already nudged recently

        // Count attempts for this app in the last hour
        val oneHourAgo = now - 3_600_000L
        val events = db.getBlockEventsForDate(UsageDatabase.todayString())
        val recentCount = events.count { event ->
            (event["app_package"] as String) == appPackage &&
            (event["timestamp"] as Long) >= oneHourAgo
        }

        if (recentCount >= threshold) {
            sendNudgeNotification(appPackage, recentCount)
            prefs.edit().putLong(KEY_LAST_NUDGE_TIMESTAMP, now).apply()
        }
    }

    // ===================================================================
    // Notifications
    // ===================================================================

    private fun createNotificationChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val streakChannel = NotificationChannel(
            STREAK_CHANNEL_ID,
            "Streak Milestones",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Celebrates screen-free streak milestones"
        }
        nm.createNotificationChannel(streakChannel)

        val nudgeChannel = NotificationChannel(
            NUDGE_CHANNEL_ID,
            "Parent Nudges",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when child repeatedly tries blocked apps"
        }
        nm.createNotificationChannel(nudgeChannel)
    }

    private fun sendStreakNotification(milestone: Int, current: Int) {
        val hours = milestone / 60
        val title = when (milestone) {
            60 -> "1 Hour Streak!"
            120 -> "2 Hour Streak!"
            240 -> "4 Hour Streak! Amazing!"
            480 -> "All-Day Champion!"
            else -> "${hours}h Streak!"
        }
        val body = when (milestone) {
            60 -> "Your child has been screen-free for over an hour. Great habits forming!"
            120 -> "Two hours without blocked apps! That's incredible progress."
            240 -> "Four hours of screen-free fun! Buddy is doing a happy dance!"
            480 -> "What an amazing day! Almost no screen time for blocked apps."
            else -> "${hours} hours without blocked apps. Keep going!"
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, STREAK_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(STREAK_NOTIFICATION_ID, notification)
        Log.d(TAG, "Streak notification: $title")
    }

    private fun sendNudgeNotification(appPackage: String, count: Int) {
        val pm = context.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(appPackage, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            appPackage.substringAfterLast('.')
        }

        val body = "Your child tried to open $appName $count times in the last hour."

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NUDGE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Heads Up, Parent!")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NUDGE_NOTIFICATION_ID, notification)
        Log.d(TAG, "Nudge notification: $appName × $count")
    }

    /**
     * Generate a contextual Buddy status message for the dashboard.
     */
    fun getBuddyStatusMessage(): String {
        val today = UsageDatabase.todayString()
        val agg = db.getDailyAggregate(today)
        val attempts = (agg?.get("total_block_attempts") as? Int) ?: 0
        val streak = getCurrentStreakInfo()
        val currentStreak = streak["current_minutes"] as Int

        return when {
            attempts == 0 && currentStreak >= 240 -> "What an incredible day! Not a single blocked app attempt. Buddy is SO proud!"
            attempts == 0 -> "Perfect so far today! No blocked app attempts. Keep it up!"
            attempts <= 3 -> "Great day! Only $attempts attempt${if (attempts == 1) "" else "s"}. Building awesome habits!"
            attempts <= 7 -> "Not bad! $attempts attempts today. Tomorrow will be even better!"
            attempts <= 15 -> "Tough day — $attempts attempts. But every day is a fresh start!"
            else -> "Busy day with $attempts attempts. Remember, progress isn't always linear. We'll keep trying!"
        }
    }

    // ===================================================================
    // Utility
    // ===================================================================

    private fun getStartOfDayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
