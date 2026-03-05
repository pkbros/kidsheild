package com.kidshield.kid_shield.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.util.Calendar

/**
 * Buddy Content Engine — selects messages for the blocking overlay mascot.
 *
 * Algorithm:
 * 1. Determine time-of-day bucket (morning / afternoon / evening / night)
 * 2. Select category via weighted random (motivational 25%, activity 30%, fun_fact 20%, moral 15%, humor 10%)
 * 3. Pick message using shuffle-bag: no immediate repeats, cycles through all messages before re-shuffling
 * 4. Cycle through Buddy poses sequentially (waving → thinking → pointing → encouraging)
 */
class BuddyContentEngine(private val context: Context) {

    companion object {
        private const val TAG = "BuddyEngine"
        private const val PREFS_NAME = "kid_shield_prefs"
        private const val KEY_SHUFFLE_BAG = "buddy_shuffle_bag"
        private const val KEY_POSE_INDEX = "buddy_pose_index"
        private const val KEY_MASCOT_ENABLED = "mascot_enabled"
        private const val KEY_OVERLAY_MODE = "overlay_mode"

        /** Overlay modes */
        const val MODE_VIDEO = "video"
        const val MODE_BUDDY = "buddy"
        const val MODE_CLASSIC = "classic"
    }

    data class BuddyContent(
        val message: String,
        val category: String,
        val pose: String
    )

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // Loaded content library
    private val categories = mutableMapOf<String, CategoryData>()
    private val poses = mutableListOf<String>()

    // Shuffle bag state per category
    private val shuffleBags = mutableMapOf<String, MutableList<Int>>()

    data class CategoryData(
        val weight: Int,
        val messages: List<MessageEntry>
    )

    data class MessageEntry(
        val text: String,
        val timeOfDay: String
    )

    init {
        loadContentLibrary()
        loadShuffleBagState()
    }

    /**
     * Whether the mascot feature is enabled in settings.
     * Backward-compatible: returns true if overlay mode is "video" or "buddy".
     */
    fun isMascotEnabled(): Boolean {
        val mode = getOverlayMode()
        return mode == MODE_VIDEO || mode == MODE_BUDDY
    }

    /**
     * Toggle mascot on/off (legacy — sets overlay mode).
     */
    fun setMascotEnabled(enabled: Boolean) {
        setOverlayMode(if (enabled) MODE_VIDEO else MODE_CLASSIC)
    }

    /**
     * Get the current overlay mode: "video", "buddy", or "classic".
     * Migrates from legacy boolean pref on first access.
     */
    fun getOverlayMode(): String {
        val mode = prefs.getString(KEY_OVERLAY_MODE, null)
        if (mode != null) return mode

        // Migrate from legacy boolean pref
        val legacyEnabled = prefs.getBoolean(KEY_MASCOT_ENABLED, true)
        val migratedMode = if (legacyEnabled) MODE_VIDEO else MODE_CLASSIC
        prefs.edit().putString(KEY_OVERLAY_MODE, migratedMode).apply()
        return migratedMode
    }

    /**
     * Set the overlay mode: "video", "buddy", or "classic".
     */
    fun setOverlayMode(mode: String) {
        prefs.edit().putString(KEY_OVERLAY_MODE, mode).apply()
        // Keep legacy pref in sync
        prefs.edit().putBoolean(KEY_MASCOT_ENABLED, mode != MODE_CLASSIC).apply()
    }

    /**
     * Get next Buddy content for the overlay.
     * Returns message, category, and pose selection.
     */
    fun getNextContent(): BuddyContent {
        val timeBucket = getCurrentTimeBucket()
        val category = selectCategory()
        val message = selectMessage(category, timeBucket)
        val pose = getNextPose()

        Log.d(TAG, "Buddy content: [$timeBucket] $category → \"$message\" (pose: $pose)")

        return BuddyContent(
            message = message,
            category = category,
            pose = pose
        )
    }

    // ─── Time-of-Day Bucketing ───

    private fun getCurrentTimeBucket(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..11 -> "morning"
            hour in 12..16 -> "afternoon"
            hour in 17..20 -> "evening"
            else -> "night" // 21-5
        }
    }

    // ─── Weighted Category Selection ───

    private fun selectCategory(): String {
        val totalWeight = categories.values.sumOf { it.weight }
        var random = (Math.random() * totalWeight).toInt()

        for ((name, data) in categories) {
            random -= data.weight
            if (random < 0) return name
        }

        // Fallback
        return categories.keys.first()
    }

    // ─── Shuffle-Bag Message Selection ───

    /**
     * Pick a message from the given category.
     * Prefers messages matching the time bucket, but falls back to "any" messages.
     * Uses shuffle-bag algorithm: all indices are shuffled, one is drawn per call.
     * When the bag empties, it's refilled and reshuffled.
     */
    private fun selectMessage(category: String, timeBucket: String): String {
        val catData = categories[category] ?: return "Hey there! Take a break and do something fun!"
        val messages = catData.messages

        if (messages.isEmpty()) return "Hey there! Take a break and do something fun!"

        // Get or create shuffle bag for this category
        val bag = shuffleBags.getOrPut(category) {
            createShuffledBag(messages.size)
        }

        // If bag is empty, refill
        if (bag.isEmpty()) {
            bag.addAll(createShuffledBag(messages.size))
        }

        // Try to find a message matching the time bucket
        // Look through the bag for a time-appropriate message
        var selectedIndex = -1
        var selectedBagPosition = -1

        for (i in bag.indices) {
            val msgIndex = bag[i]
            val msg = messages[msgIndex]
            if (msg.timeOfDay == timeBucket || msg.timeOfDay == "any") {
                selectedIndex = msgIndex
                selectedBagPosition = i
                // Prefer time-specific messages
                if (msg.timeOfDay == timeBucket) break
            }
        }

        // If no match found (unlikely), just take the first from bag
        if (selectedIndex == -1) {
            selectedIndex = bag.removeAt(0)
        } else {
            bag.removeAt(selectedBagPosition)
        }

        // Persist shuffle bag state
        saveShuffleBagState()

        return messages[selectedIndex].text
    }

    private fun createShuffledBag(size: Int): MutableList<Int> {
        val indices = (0 until size).toMutableList()
        indices.shuffle()
        return indices
    }

    // ─── Pose Cycling ───

    private fun getNextPose(): String {
        if (poses.isEmpty()) return "waving"

        val currentIndex = prefs.getInt(KEY_POSE_INDEX, 0)
        val pose = poses[currentIndex % poses.size]
        prefs.edit().putInt(KEY_POSE_INDEX, (currentIndex + 1) % poses.size).apply()
        return pose
    }

    // ─── Content Library Loading ───

    private fun loadContentLibrary() {
        try {
            val jsonString = context.assets.open("buddy_content.json")
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(jsonString)
            val categoriesObj = root.getJSONObject("categories")

            for (key in categoriesObj.keys()) {
                val catObj = categoriesObj.getJSONObject(key)
                val weight = catObj.getInt("weight")
                val messagesArr = catObj.getJSONArray("messages")
                val messages = mutableListOf<MessageEntry>()

                for (i in 0 until messagesArr.length()) {
                    val msgObj = messagesArr.getJSONObject(i)
                    messages.add(MessageEntry(
                        text = msgObj.getString("text"),
                        timeOfDay = msgObj.getString("timeOfDay")
                    ))
                }

                categories[key] = CategoryData(weight = weight, messages = messages)
            }

            // Load poses
            val posesArr = root.getJSONArray("poses")
            for (i in 0 until posesArr.length()) {
                poses.add(posesArr.getString(i))
            }

            val totalMessages = categories.values.sumOf { it.messages.size }
            Log.d(TAG, "Loaded content library: $totalMessages messages in ${categories.size} categories, ${poses.size} poses")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load buddy content library", e)
            // Fallback: ensure at least one category with one message
            categories["motivational"] = CategoryData(
                weight = 100,
                messages = listOf(MessageEntry("Hey! Take a break and do something awesome!", "any"))
            )
            poses.addAll(listOf("waving", "thinking", "pointing", "encouraging"))
        }
    }

    // ─── Shuffle Bag Persistence ───

    private fun loadShuffleBagState() {
        try {
            val json = prefs.getString(KEY_SHUFFLE_BAG, null) ?: return
            val type = object : TypeToken<Map<String, List<Int>>>() {}.type
            val saved: Map<String, List<Int>> = gson.fromJson(json, type) ?: return
            for ((key, indices) in saved) {
                shuffleBags[key] = indices.toMutableList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load shuffle bag state, starting fresh", e)
        }
    }

    private fun saveShuffleBagState() {
        try {
            val json = gson.toJson(shuffleBags)
            prefs.edit().putString(KEY_SHUFFLE_BAG, json).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save shuffle bag state", e)
        }
    }
}
