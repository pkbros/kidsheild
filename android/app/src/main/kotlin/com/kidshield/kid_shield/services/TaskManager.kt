package com.kidshield.kid_shield.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Manages parent-assigned daily habit tasks that children complete
 * to earn more screen time after hitting their daily limit.
 *
 * Flow:
 * 1. Parent creates tasks (e.g., "Do homework", "Learn a rhyme")
 * 2. Child hits daily limit → overlay shows task list
 * 3. Child completes a task offline → taps "Done"
 * 4. Parent verifies completion via PIN → child earns bonus time
 */
class TaskManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TaskManager"
        private const val PREFS_NAME = "kid_shield_tasks"
        private const val KEY_TASKS = "daily_tasks"
        private const val KEY_COMPLETED_TODAY = "completed_today"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_BONUS_MINUTES_PER_TASK = "bonus_minutes_per_task"

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fun todayString(): String = dateFormat.format(Date())

        @Volatile
        private var instance: TaskManager? = null

        fun getInstance(context: Context): TaskManager {
            return instance ?: synchronized(this) {
                instance ?: TaskManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        resetIfNewDay()
    }

    // ─── Task CRUD ────────────────────────────────────────

    data class Task(
        val id: String,
        val title: String,
        val description: String = "",
        val bonusMinutes: Int = 15
    )

    /**
     * Get all parent-defined tasks.
     */
    fun getTasks(): List<Task> {
        val json = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        val type = object : TypeToken<List<Task>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /**
     * Add a new task. Returns the created task.
     */
    fun addTask(title: String, description: String = "", bonusMinutes: Int = 0): Task {
        val effectiveBonus = if (bonusMinutes > 0) bonusMinutes else getBonusMinutesPerTask()
        val task = Task(
            id = UUID.randomUUID().toString().take(8),
            title = title,
            description = description,
            bonusMinutes = effectiveBonus
        )
        val tasks = getTasks().toMutableList()
        tasks.add(task)
        saveTasks(tasks)
        Log.d(TAG, "Added task: ${task.title} (+${task.bonusMinutes} min)")
        return task
    }

    /**
     * Remove a task by ID.
     */
    fun removeTask(taskId: String) {
        val tasks = getTasks().toMutableList()
        tasks.removeAll { it.id == taskId }
        saveTasks(tasks)
        // Also remove from completed if it was completed today
        val completed = getCompletedTodayIds().toMutableSet()
        completed.remove(taskId)
        saveCompletedToday(completed)
    }

    /**
     * Update an existing task.
     */
    fun updateTask(taskId: String, title: String, description: String, bonusMinutes: Int) {
        val tasks = getTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            tasks[index] = Task(taskId, title, description, bonusMinutes)
            saveTasks(tasks)
        }
    }

    private fun saveTasks(tasks: List<Task>) {
        prefs.edit().putString(KEY_TASKS, gson.toJson(tasks)).apply()
    }

    // ─── Daily Reset ──────────────────────────────────────

    private fun resetIfNewDay() {
        val lastReset = prefs.getString(KEY_LAST_RESET_DATE, "") ?: ""
        val today = todayString()
        if (lastReset != today) {
            Log.d(TAG, "New day detected ($lastReset → $today), resetting completed tasks")
            prefs.edit()
                .putString(KEY_COMPLETED_TODAY, gson.toJson(emptySet<String>()))
                .putString(KEY_LAST_RESET_DATE, today)
                .apply()
        }
    }

    // ─── Task Completion ──────────────────────────────────

    /**
     * Get IDs of tasks completed today.
     */
    fun getCompletedTodayIds(): Set<String> {
        resetIfNewDay()
        val json = prefs.getString(KEY_COMPLETED_TODAY, "[]") ?: "[]"
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson(json, type) ?: emptySet()
    }

    /**
     * Mark a task as completed (after parent PIN verification).
     * Returns the bonus minutes earned, or 0 if already completed.
     */
    fun completeTask(taskId: String): Int {
        resetIfNewDay()
        val completed = getCompletedTodayIds().toMutableSet()
        if (completed.contains(taskId)) {
            Log.d(TAG, "Task $taskId already completed today")
            return 0
        }

        val task = getTasks().find { it.id == taskId } ?: return 0
        completed.add(taskId)
        saveCompletedToday(completed)

        // Log to usage database for analytics
        try {
            val db = UsageDatabase.getInstance(context)
            db.logTaskCompletion(taskId, task.title, task.bonusMinutes)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log task completion to DB", e)
        }

        Log.d(TAG, "Task completed: ${task.title} → +${task.bonusMinutes} min earned")
        return task.bonusMinutes
    }

    /**
     * Check if a specific task is completed today.
     */
    fun isTaskCompletedToday(taskId: String): Boolean {
        return getCompletedTodayIds().contains(taskId)
    }

    private fun saveCompletedToday(completed: Set<String>) {
        prefs.edit().putString(KEY_COMPLETED_TODAY, gson.toJson(completed)).apply()
    }

    // ─── Status ────────────────────────────────────────────

    /**
     * Get full task status for today: tasks with completion state.
     */
    fun getTodayTaskStatus(): List<Map<String, Any>> {
        val tasks = getTasks()
        val completed = getCompletedTodayIds()
        return tasks.map { task ->
            mapOf(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "bonusMinutes" to task.bonusMinutes,
                "completedToday" to completed.contains(task.id)
            )
        }
    }

    /**
     * Get total bonus minutes earned today from completed tasks.
     */
    fun getTodayEarnedMinutes(): Int {
        val tasks = getTasks()
        val completed = getCompletedTodayIds()
        return tasks.filter { completed.contains(it.id) }
            .sumOf { it.bonusMinutes }
    }

    /**
     * Get count of pending (incomplete) tasks for today.
     */
    fun getPendingTaskCount(): Int {
        val tasks = getTasks()
        val completed = getCompletedTodayIds()
        return tasks.count { !completed.contains(it.id) }
    }

    // ─── Settings ──────────────────────────────────────────

    fun getBonusMinutesPerTask(): Int {
        return prefs.getInt(KEY_BONUS_MINUTES_PER_TASK, 15)
    }

    fun setBonusMinutesPerTask(minutes: Int) {
        prefs.edit().putInt(KEY_BONUS_MINUTES_PER_TASK, minutes.coerceIn(5, 120)).apply()
    }
}
