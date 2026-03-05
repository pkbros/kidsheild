package com.kidshield.kid_shield.services

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Local SQLite database for usage analytics.
 *
 * Tables:
 *  - block_events: individual overlay trigger events with outcome
 *  - daily_aggregates: per-day summary stats
 *  - streaks: screen-free streak periods
 *  - screen_time: daily screen time (child + parent contexts)
 */
class UsageDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val TAG = "UsageDatabase"
        private const val DB_NAME = "usage.db"
        private const val DB_VERSION = 1

        // Retain 90 days of history
        private const val RETENTION_DAYS = 90

        @Volatile
        private var instance: UsageDatabase? = null

        fun getInstance(context: Context): UsageDatabase {
            return instance ?: synchronized(this) {
                instance ?: UsageDatabase(context.applicationContext).also { instance = it }
            }
        }

        // Date format helper
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fun todayString(): String = dateFormat.format(Date())
        fun dateString(timeMs: Long): String = dateFormat.format(Date(timeMs))
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE block_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                app_package TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                outcome TEXT NOT NULL DEFAULT 'navigated_away'
            )
        """)

        db.execSQL("""
            CREATE TABLE daily_aggregates (
                date TEXT PRIMARY KEY,
                total_block_attempts INTEGER NOT NULL DEFAULT 0,
                total_verified INTEGER NOT NULL DEFAULT 0,
                total_screen_time_minutes INTEGER NOT NULL DEFAULT 0,
                parent_screen_time_minutes INTEGER,
                best_streak_minutes INTEGER NOT NULL DEFAULT 0,
                most_attempted_app TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE streaks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                duration_minutes INTEGER NOT NULL DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE screen_time (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                context TEXT NOT NULL,
                total_minutes INTEGER NOT NULL DEFAULT 0,
                UNIQUE(date, context) ON CONFLICT REPLACE
            )
        """)

        // Indexes for common queries
        db.execSQL("CREATE INDEX idx_block_events_timestamp ON block_events(timestamp)")
        db.execSQL("CREATE INDEX idx_block_events_date ON block_events(app_package, timestamp)")
        db.execSQL("CREATE INDEX idx_streaks_start ON streaks(start_time)")

        Log.d(TAG, "Database created with all tables")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations go here
        Log.d(TAG, "Database upgrade from $oldVersion to $newVersion")
    }

    // ===================================================================
    // Block Events
    // ===================================================================

    /**
     * Record a new block event when the overlay is triggered.
     * Returns the row ID (used to update outcome later).
     */
    fun insertBlockEvent(appPackage: String, timestampMs: Long = System.currentTimeMillis()): Long {
        val values = ContentValues().apply {
            put("app_package", appPackage)
            put("timestamp", timestampMs)
            put("outcome", "navigated_away") // default until updated
        }
        val id = writableDatabase.insert("block_events", null, values)
        Log.d(TAG, "Inserted block event id=$id for $appPackage")
        return id
    }

    /**
     * Update the outcome of a block event (face_verified, pin_verified, navigated_away).
     */
    fun updateBlockEventOutcome(eventId: Long, outcome: String) {
        val values = ContentValues().apply { put("outcome", outcome) }
        writableDatabase.update("block_events", values, "id = ?", arrayOf(eventId.toString()))
        Log.d(TAG, "Updated block event id=$eventId outcome=$outcome")
    }

    /**
     * Get block events for a specific date (YYYY-MM-DD).
     */
    fun getBlockEventsForDate(date: String): List<Map<String, Any>> {
        val startMs = dateToStartOfDayMs(date)
        val endMs = startMs + 86_400_000L
        val events = mutableListOf<Map<String, Any>>()

        readableDatabase.rawQuery(
            "SELECT id, app_package, timestamp, outcome FROM block_events WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp DESC",
            arrayOf(startMs.toString(), endMs.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                events.add(mapOf(
                    "id" to cursor.getLong(0),
                    "app_package" to cursor.getString(1),
                    "timestamp" to cursor.getLong(2),
                    "outcome" to cursor.getString(3)
                ))
            }
        }
        return events
    }

    /**
     * Get the most recent block event ID for a given app package
     * (used to update outcome when verification succeeds).
     */
    fun getLatestBlockEventId(appPackage: String): Long? {
        readableDatabase.rawQuery(
            "SELECT id FROM block_events WHERE app_package = ? ORDER BY timestamp DESC LIMIT 1",
            arrayOf(appPackage)
        ).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    /**
     * Get top blocked apps for a date range, ordered by attempt count.
     */
    fun getTopBlockedApps(startDate: String, endDate: String, limit: Int = 5): List<Map<String, Any>> {
        val startMs = dateToStartOfDayMs(startDate)
        val endMs = dateToStartOfDayMs(endDate) + 86_400_000L
        val apps = mutableListOf<Map<String, Any>>()

        readableDatabase.rawQuery(
            """SELECT app_package, COUNT(*) as attempts,
               SUM(CASE WHEN outcome IN ('face_verified','pin_verified') THEN 1 ELSE 0 END) as verified
               FROM block_events WHERE timestamp >= ? AND timestamp < ?
               GROUP BY app_package ORDER BY attempts DESC LIMIT ?""",
            arrayOf(startMs.toString(), endMs.toString(), limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                apps.add(mapOf(
                    "app_package" to cursor.getString(0),
                    "attempts" to cursor.getInt(1),
                    "verified" to cursor.getInt(2)
                ))
            }
        }
        return apps
    }

    // ===================================================================
    // Daily Aggregates
    // ===================================================================

    /**
     * Compute and store daily aggregates for the given date.
     * Called at end-of-day or on dashboard load.
     */
    fun computeDailyAggregate(date: String) {
        val startMs = dateToStartOfDayMs(date)
        val endMs = startMs + 86_400_000L
        val db = writableDatabase

        // Count block attempts
        var totalAttempts = 0
        var totalVerified = 0
        var mostAttemptedApp: String? = null

        db.rawQuery(
            "SELECT COUNT(*) FROM block_events WHERE timestamp >= ? AND timestamp < ?",
            arrayOf(startMs.toString(), endMs.toString())
        ).use { if (it.moveToFirst()) totalAttempts = it.getInt(0) }

        db.rawQuery(
            "SELECT COUNT(*) FROM block_events WHERE timestamp >= ? AND timestamp < ? AND outcome IN ('face_verified','pin_verified')",
            arrayOf(startMs.toString(), endMs.toString())
        ).use { if (it.moveToFirst()) totalVerified = it.getInt(0) }

        db.rawQuery(
            "SELECT app_package FROM block_events WHERE timestamp >= ? AND timestamp < ? GROUP BY app_package ORDER BY COUNT(*) DESC LIMIT 1",
            arrayOf(startMs.toString(), endMs.toString())
        ).use { if (it.moveToFirst()) mostAttemptedApp = it.getString(0) }

        // Get screen time
        var childScreenTime = 0
        db.rawQuery(
            "SELECT total_minutes FROM screen_time WHERE date = ? AND context = 'child'",
            arrayOf(date)
        ).use { if (it.moveToFirst()) childScreenTime = it.getInt(0) }

        var parentScreenTime: Int? = null
        db.rawQuery(
            "SELECT total_minutes FROM screen_time WHERE date = ? AND context = 'parent'",
            arrayOf(date)
        ).use { if (it.moveToFirst()) parentScreenTime = it.getInt(0) }

        // Get best streak for the day
        var bestStreakMinutes = 0
        db.rawQuery(
            """SELECT MAX(duration_minutes) FROM streaks
               WHERE start_time >= ? AND start_time < ?""",
            arrayOf(startMs.toString(), endMs.toString())
        ).use { if (it.moveToFirst()) bestStreakMinutes = it.getInt(0) }

        // Upsert
        val values = ContentValues().apply {
            put("date", date)
            put("total_block_attempts", totalAttempts)
            put("total_verified", totalVerified)
            put("total_screen_time_minutes", childScreenTime)
            if (parentScreenTime != null) put("parent_screen_time_minutes", parentScreenTime)
            put("best_streak_minutes", bestStreakMinutes)
            put("most_attempted_app", mostAttemptedApp)
        }
        db.insertWithOnConflict("daily_aggregates", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        Log.d(TAG, "Computed daily aggregate for $date: attempts=$totalAttempts verified=$totalVerified streak=${bestStreakMinutes}m")
    }

    /**
     * Get daily aggregate for a specific date.
     */
    fun getDailyAggregate(date: String): Map<String, Any?>? {
        readableDatabase.rawQuery(
            "SELECT * FROM daily_aggregates WHERE date = ?", arrayOf(date)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return mapOf(
                    "date" to cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    "total_block_attempts" to cursor.getInt(cursor.getColumnIndexOrThrow("total_block_attempts")),
                    "total_verified" to cursor.getInt(cursor.getColumnIndexOrThrow("total_verified")),
                    "total_screen_time_minutes" to cursor.getInt(cursor.getColumnIndexOrThrow("total_screen_time_minutes")),
                    "parent_screen_time_minutes" to if (cursor.isNull(cursor.getColumnIndexOrThrow("parent_screen_time_minutes"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("parent_screen_time_minutes")),
                    "best_streak_minutes" to cursor.getInt(cursor.getColumnIndexOrThrow("best_streak_minutes")),
                    "most_attempted_app" to cursor.getString(cursor.getColumnIndexOrThrow("most_attempted_app"))
                )
            }
        }
        return null
    }

    /**
     * Get daily aggregates for a date range (for weekly summary).
     */
    fun getDailyAggregates(startDate: String, endDate: String): List<Map<String, Any?>> {
        val aggregates = mutableListOf<Map<String, Any?>>()
        readableDatabase.rawQuery(
            "SELECT * FROM daily_aggregates WHERE date >= ? AND date <= ? ORDER BY date ASC",
            arrayOf(startDate, endDate)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                aggregates.add(mapOf(
                    "date" to cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    "total_block_attempts" to cursor.getInt(cursor.getColumnIndexOrThrow("total_block_attempts")),
                    "total_verified" to cursor.getInt(cursor.getColumnIndexOrThrow("total_verified")),
                    "total_screen_time_minutes" to cursor.getInt(cursor.getColumnIndexOrThrow("total_screen_time_minutes")),
                    "parent_screen_time_minutes" to if (cursor.isNull(cursor.getColumnIndexOrThrow("parent_screen_time_minutes"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("parent_screen_time_minutes")),
                    "best_streak_minutes" to cursor.getInt(cursor.getColumnIndexOrThrow("best_streak_minutes")),
                    "most_attempted_app" to cursor.getString(cursor.getColumnIndexOrThrow("most_attempted_app"))
                ))
            }
        }
        return aggregates
    }

    // ===================================================================
    // Streaks
    // ===================================================================

    /**
     * Start a new screen-free streak. Returns the streak ID.
     */
    fun startStreak(startTimeMs: Long = System.currentTimeMillis()): Long {
        val values = ContentValues().apply {
            put("start_time", startTimeMs)
            put("duration_minutes", 0)
        }
        val id = writableDatabase.insert("streaks", null, values)
        Log.d(TAG, "Started new streak id=$id")
        return id
    }

    /**
     * End an active streak. Calculates and stores the duration.
     */
    fun endStreak(streakId: Long, endTimeMs: Long = System.currentTimeMillis()) {
        // Get start time
        var startTime = 0L
        readableDatabase.rawQuery(
            "SELECT start_time FROM streaks WHERE id = ?", arrayOf(streakId.toString())
        ).use { if (it.moveToFirst()) startTime = it.getLong(0) }

        if (startTime == 0L) return

        val durationMinutes = ((endTimeMs - startTime) / 60_000).toInt()
        val values = ContentValues().apply {
            put("end_time", endTimeMs)
            put("duration_minutes", durationMinutes)
        }
        writableDatabase.update("streaks", values, "id = ?", arrayOf(streakId.toString()))
        Log.d(TAG, "Ended streak id=$streakId duration=${durationMinutes}m")
    }

    /**
     * Get the current active streak (end_time IS NULL), if any.
     */
    fun getActiveStreak(): Map<String, Any>? {
        readableDatabase.rawQuery(
            "SELECT id, start_time FROM streaks WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val startTime = cursor.getLong(1)
                val durationMinutes = ((System.currentTimeMillis() - startTime) / 60_000).toInt()
                return mapOf(
                    "id" to cursor.getLong(0),
                    "start_time" to startTime,
                    "duration_minutes" to durationMinutes
                )
            }
        }
        return null
    }

    /**
     * Get today's best completed streak duration in minutes.
     */
    fun getTodayBestStreak(): Int {
        val today = todayString()
        val startMs = dateToStartOfDayMs(today)
        val endMs = startMs + 86_400_000L
        var best = 0

        readableDatabase.rawQuery(
            "SELECT MAX(duration_minutes) FROM streaks WHERE start_time >= ? AND start_time < ? AND end_time IS NOT NULL",
            arrayOf(startMs.toString(), endMs.toString())
        ).use { if (it.moveToFirst()) best = it.getInt(0) }

        // Also check active streak
        val active = getActiveStreak()
        if (active != null) {
            val activeDuration = active["duration_minutes"] as Int
            if (activeDuration > best) best = activeDuration
        }

        return best
    }

    /**
     * Get the all-time longest streak in minutes.
     */
    fun getLongestStreakEver(): Int {
        var longest = 0
        readableDatabase.rawQuery(
            "SELECT MAX(duration_minutes) FROM streaks WHERE end_time IS NOT NULL", null
        ).use { if (it.moveToFirst()) longest = it.getInt(0) }

        // Also check active streak
        val active = getActiveStreak()
        if (active != null) {
            val activeDuration = active["duration_minutes"] as Int
            if (activeDuration > longest) longest = activeDuration
        }

        return longest
    }

    // ===================================================================
    // Screen Time
    // ===================================================================

    /**
     * Update screen time for a given date and context (child/parent).
     */
    fun setScreenTime(date: String, contextType: String, totalMinutes: Int) {
        val values = ContentValues().apply {
            put("date", date)
            put("context", contextType)
            put("total_minutes", totalMinutes)
        }
        writableDatabase.insertWithOnConflict("screen_time", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Get screen time for a given date and context.
     */
    fun getScreenTime(date: String, contextType: String): Int {
        readableDatabase.rawQuery(
            "SELECT total_minutes FROM screen_time WHERE date = ? AND context = ?",
            arrayOf(date, contextType)
        ).use { if (it.moveToFirst()) return it.getInt(0) }
        return 0
    }

    // ===================================================================
    // Maintenance
    // ===================================================================

    /**
     * Prune data older than RETENTION_DAYS. Call periodically.
     */
    fun pruneOldData() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -RETENTION_DAYS)
        val cutoffMs = cal.timeInMillis
        val cutoffDate = dateFormat.format(cal.time)
        val db = writableDatabase

        val eventsDeleted = db.delete("block_events", "timestamp < ?", arrayOf(cutoffMs.toString()))
        val streaksDeleted = db.delete("streaks", "start_time < ? AND end_time IS NOT NULL", arrayOf(cutoffMs.toString()))
        val aggDeleted = db.delete("daily_aggregates", "date < ?", arrayOf(cutoffDate))
        val stDeleted = db.delete("screen_time", "date < ?", arrayOf(cutoffDate))

        if (eventsDeleted + streaksDeleted + aggDeleted + stDeleted > 0) {
            Log.d(TAG, "Pruned old data: events=$eventsDeleted streaks=$streaksDeleted agg=$aggDeleted screenTime=$stDeleted")
        }
    }

    // ===================================================================
    // Utility
    // ===================================================================

    private fun dateToStartOfDayMs(dateStr: String): Long {
        return try {
            val date = dateFormat.parse(dateStr)
            val cal = Calendar.getInstance()
            cal.time = date!!
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: $dateStr", e)
            0L
        }
    }
}
