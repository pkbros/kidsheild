package com.kidshield.kid_shield.channels

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kidshield.kid_shield.receivers.KidShieldDeviceAdminReceiver
import com.kidshield.kid_shield.services.BuddyContentEngine
import com.kidshield.kid_shield.services.FaceRecognitionEngine
import com.kidshield.kid_shield.services.KidShieldAccessibilityService
import com.kidshield.kid_shield.services.KidShieldForegroundService
import com.kidshield.kid_shield.services.UsageTrackingEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class PlatformChannelHandler(
    private val context: Context,
    private val activity: android.app.Activity
) : MethodChannel.MethodCallHandler {

    companion object {
        const val CHANNEL_NAME = "com.kidshield.kid_shield/platform"
        private const val TAG = "PlatformChannel"
        private const val PREFS_NAME = "kid_shield_prefs"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private var faceEngine: FaceRecognitionEngine? = null
    private var buddyEngine: BuddyContentEngine? = null
    private var trackingEngine: UsageTrackingEngine? = null

    init {
        // Migrate old minutes-based interval to seconds-based
        if (prefs.contains("reverification_interval_minutes") && !prefs.contains("reverification_interval_seconds")) {
            val oldMinutes = prefs.getInt("reverification_interval_minutes", 30)
            prefs.edit()
                .putInt("reverification_interval_seconds", oldMinutes * 60)
                .remove("reverification_interval_minutes")
                .apply()
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            // === Permission Checks ===
            "checkAccessibilityPermission" -> {
                result.success(isAccessibilityEnabled())
            }
            "requestAccessibilityPermission" -> {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                result.success(true)
            }
            "checkOverlayPermission" -> {
                result.success(Settings.canDrawOverlays(context))
            }
            "requestOverlayPermission" -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                result.success(true)
            }
            "checkDeviceAdminPermission" -> {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val componentName = ComponentName(context, KidShieldDeviceAdminReceiver::class.java)
                result.success(dpm.isAdminActive(componentName))
            }
            "requestDeviceAdminPermission" -> {
                val componentName = ComponentName(context, KidShieldDeviceAdminReceiver::class.java)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "KidShield needs device admin access to prevent children from uninstalling the app."
                    )
                }
                activity.startActivityForResult(intent, 1001)
                result.success(true)
            }
            "checkBatteryOptimization" -> {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                result.success(pm.isIgnoringBatteryOptimizations(context.packageName))
            }
            "requestBatteryOptimization" -> {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                result.success(true)
            }
            "checkUsageStatsPermission" -> {
                result.success(hasUsageStatsPermission())
            }
            "requestUsageStatsPermission" -> {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                result.success(true)
            }

            // === Service Control ===
            "startMonitoringService" -> {
                startForegroundService()
                result.success(true)
            }
            "stopMonitoringService" -> {
                val intent = Intent(context, KidShieldForegroundService::class.java)
                context.stopService(intent)
                result.success(true)
            }
            "isMonitoringServiceRunning" -> {
                result.success(KidShieldForegroundService.isRunning)
            }

            // === Installed Apps ===
            "getInstalledApps" -> {
                val apps = getInstalledApps()
                result.success(apps)
            }

            // === Blocked Apps ===
            "getBlockedApps" -> {
                val json = prefs.getString("blocked_apps", "[]") ?: "[]"
                val type = object : TypeToken<List<String>>() {}.type
                val list: List<String> = gson.fromJson(json, type)
                result.success(list)
            }
            "setBlockedApps" -> {
                val apps = call.argument<List<String>>("apps") ?: emptyList()
                prefs.edit().putString("blocked_apps", gson.toJson(apps)).apply()
                result.success(true)
            }

            // === PIN ===
            "setPin" -> {
                val pin = call.argument<String>("pin") ?: ""
                if (pin.length < 6) {
                    result.error("INVALID_PIN", "PIN must be at least 6 digits", null)
                    return
                }
                val hash = hashPin(pin)
                prefs.edit().putString("pin_hash", hash).apply()
                result.success(true)
            }
            "verifyPin" -> {
                val pin = call.argument<String>("pin") ?: ""
                val storedHash = prefs.getString("pin_hash", null)
                val enteredHash = hashPin(pin)
                result.success(enteredHash == storedHash)
            }
            "isPinSet" -> {
                result.success(prefs.getString("pin_hash", null) != null)
            }

            // === Face Registration ===
            "initFaceEngine" -> {
                if (faceEngine == null) {
                    faceEngine = FaceRecognitionEngine(context)
                }
                result.success(true)
            }
            "processAndRegisterFace" -> {
                val imageBytes = call.argument<ByteArray>("imageBytes")
                val parentName = call.argument<String>("name") ?: "Parent"
                if (imageBytes == null) {
                    result.error("NO_IMAGE", "No image data provided", null)
                    return
                }

                val engine = faceEngine ?: FaceRecognitionEngine(context).also { faceEngine = it }
                val bitmap = decodeBitmapWithExifRotation(imageBytes)
                if (bitmap == null) {
                    result.error("DECODE_ERROR", "Failed to decode image", null)
                    return
                }

                Log.d(TAG, "Register face: bitmap ${bitmap.width}x${bitmap.height}")
                engine.processFrame(bitmap) { embedding ->
                    activity.runOnUiThread {
                        if (embedding != null) {
                            saveEmbedding(parentName, embedding)
                            result.success(mapOf(
                                "success" to true,
                                "embeddingSize" to embedding.size
                            ))
                        } else {
                            result.success(mapOf(
                                "success" to false,
                                "error" to "No face detected in image"
                            ))
                        }
                    }
                }
            }
            "getRegisteredFaces" -> {
                val faces = getRegisteredFaceNames()
                result.success(faces)
            }
            "deleteRegisteredFace" -> {
                val index = call.argument<Int>("index") ?: -1
                deleteFace(index)
                result.success(true)
            }
            "verifyFace" -> {
                val imageBytes = call.argument<ByteArray>("imageBytes")
                if (imageBytes == null) {
                    result.error("NO_IMAGE", "No image data", null)
                    return
                }

                val engine = faceEngine ?: FaceRecognitionEngine(context).also { faceEngine = it }
                val bitmap = decodeBitmapWithExifRotation(imageBytes)
                if (bitmap == null) {
                    result.error("DECODE_ERROR", "Failed to decode image", null)
                    return
                }

                val registeredEmbeddings = loadRegisteredEmbeddings()
                if (registeredEmbeddings.isEmpty()) {
                    result.success(mapOf("matched" to false, "error" to "No faces registered"))
                    return
                }

                engine.processFrame(bitmap) { embedding ->
                    activity.runOnUiThread {
                        if (embedding != null) {
                            val (matchIndex, similarity) = engine.matchAgainstRegistered(
                                embedding, registeredEmbeddings
                            )
                            result.success(mapOf(
                                "matched" to (matchIndex >= 0),
                                "parentIndex" to matchIndex,
                                "similarity" to similarity
                            ))
                        } else {
                            result.success(mapOf("matched" to false, "error" to "No face detected"))
                        }
                    }
                }
            }

            // === Settings ===
            "getReverificationInterval" -> {
                result.success(prefs.getInt("reverification_interval_seconds", 1800))
            }
            "setReverificationInterval" -> {
                val interval = call.argument<Int>("interval") ?: 1800
                prefs.edit().putInt("reverification_interval_seconds", interval).apply()
                result.success(true)
            }
            "getRecheckInterval" -> {
                result.success(prefs.getInt("recheck_interval_minutes", 1))
            }
            "setRecheckInterval" -> {
                val interval = call.argument<Int>("interval") ?: 1
                prefs.edit().putInt("recheck_interval_minutes", interval).apply()
                result.success(true)
            }
            "isProtectionEnabled" -> {
                result.success(prefs.getBoolean("is_protection_enabled", false))
            }
            "setProtectionEnabled" -> {
                val enabled = call.argument<Boolean>("enabled") ?: false
                prefs.edit().putBoolean("is_protection_enabled", enabled).apply()
                if (enabled) {
                    startForegroundService()
                } else {
                    val intent = Intent(context, KidShieldForegroundService::class.java)
                    context.stopService(intent)
                }
                result.success(true)
            }
            "isSetupComplete" -> {
                result.success(prefs.getBoolean("is_setup_complete", false))
            }
            "setSetupComplete" -> {
                val complete = call.argument<Boolean>("complete") ?: false
                prefs.edit().putBoolean("is_setup_complete", complete).apply()
                result.success(true)
            }
            "isDebugMode" -> {
                result.success(prefs.getBoolean("debug_mode", false))
            }
            "setDebugMode" -> {
                val enabled = call.argument<Boolean>("enabled") ?: false
                prefs.edit().putBoolean("debug_mode", enabled).apply()
                result.success(true)
            }
            "clearVerificationSessions" -> {
                prefs.edit().putString("verification_sessions", "{}").apply()
                result.success(true)
            }

            // === Mascot / Buddy ===
            "isMascotEnabled" -> {
                val engine = buddyEngine ?: BuddyContentEngine(context).also { buddyEngine = it }
                result.success(engine.isMascotEnabled())
            }
            "setMascotEnabled" -> {
                val enabled = call.argument<Boolean>("enabled") ?: true
                val engine = buddyEngine ?: BuddyContentEngine(context).also { buddyEngine = it }
                engine.setMascotEnabled(enabled)
                result.success(true)
            }
            "getOverlayMode" -> {
                val engine = buddyEngine ?: BuddyContentEngine(context).also { buddyEngine = it }
                result.success(engine.getOverlayMode())
            }
            "setOverlayMode" -> {
                val mode = call.argument<String>("mode") ?: BuddyContentEngine.MODE_VIDEO
                val engine = buddyEngine ?: BuddyContentEngine(context).also { buddyEngine = it }
                engine.setOverlayMode(mode)
                result.success(true)
            }

            // === Analytics / Usage Tracking ===
            "getTodayStats" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                try {
                    val stats = engine.getTodayStats()
                    result.success(stats)
                } catch (e: Exception) {
                    Log.e(TAG, "getTodayStats failed", e)
                    result.success(emptyMap<String, Any>())
                }
            }
            "getYesterdayStats" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.getYesterdayStats() ?: emptyMap<String, Any>())
            }
            "getWeeklySummary" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.getWeeklySummary())
            }
            "getCurrentStreakInfo" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.getCurrentStreakInfo())
            }
            "getBlockEventsForDate" -> {
                val date = call.argument<String>("date") ?: ""
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.getBlockEventsForDate(date))
            }
            "getBuddyStatusMessage" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.getBuddyStatusMessage())
            }
            "refreshTodayAggregate" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                engine.refreshTodayAggregate()
                result.success(true)
            }

            // === Parent Self-Awareness ===
            "isParentTrackingEnabled" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.isParentTrackingEnabled())
            }
            "setParentTrackingEnabled" -> {
                val enabled = call.argument<Boolean>("enabled") ?: false
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                engine.setParentTrackingEnabled(enabled)
                result.success(true)
            }
            "getParentScreenTimeGoal" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.getParentScreenTimeGoal())
            }
            "setParentScreenTimeGoal" -> {
                val minutes = call.argument<Int>("minutes") ?: 0
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                engine.setParentScreenTimeGoal(minutes)
                result.success(true)
            }
            "getParentInsight" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.getParentInsight())
            }

            // === Nudge Settings ===
            "getNudgeThreshold" -> {
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                result.success(engine.getNudgeThreshold())
            }
            "setNudgeThreshold" -> {
                val threshold = call.argument<Int>("threshold") ?: 5
                val engine = trackingEngine ?: UsageTrackingEngine.getInstance(context).also { trackingEngine = it }
                engine.setNudgeThreshold(threshold)
                result.success(true)
            }

            // === Detection Mode Status ===
            "getDetectionMode" -> {
                val a11yEnabled = isAccessibilityEnabled()
                val usageGranted = hasUsageStatsPermission()
                val mode = when {
                    usageGranted && a11yEnabled -> "hybrid"       // Both active: polling + instant a11y
                    usageGranted -> "polling"                     // Primary: UsageStats polling
                    a11yEnabled -> "accessibility"                // Legacy: a11y only
                    else -> "none"                                // No detection available
                }
                result.success(mapOf(
                    "mode" to mode,
                    "usageStatsGranted" to usageGranted,
                    "accessibilityEnabled" to a11yEnabled,
                    "pollingActive" to (KidShieldForegroundService.isRunning && usageGranted)
                ))
            }

            else -> result.notImplemented()
        }
    }

    // === Helper Methods ===

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${context.packageName}/${context.packageName}.services.KidShieldAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(service, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun startForegroundService() {
        val intent = Intent(context, KidShieldForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun getInstalledApps(): List<Map<String, Any>> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // System packages to exclude
        val excludedPackages = setOf(
            "com.android.settings",
            "com.android.phone",
            "com.android.dialer",
            "com.android.contacts",
            "com.android.mms",
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            context.packageName // Exclude ourselves
        )

        return packages
            .filter { app ->
                // Include if it has a launcher intent (user-visible app)
                pm.getLaunchIntentForPackage(app.packageName) != null &&
                !excludedPackages.contains(app.packageName)
            }
            .map { app ->
                val appName = pm.getApplicationLabel(app).toString()
                // Get app icon as base64
                val iconBase64 = try {
                    val icon = pm.getApplicationIcon(app)
                    val bitmap = if (icon is android.graphics.drawable.BitmapDrawable) {
                        icon.bitmap
                    } else {
                        val bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bmp)
                        icon.setBounds(0, 0, 48, 48)
                        icon.draw(canvas)
                        bmp
                    }
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
                    Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                } catch (e: Exception) {
                    ""
                }

                mapOf(
                    "packageName" to app.packageName,
                    "appName" to appName,
                    "iconBase64" to iconBase64,
                    "isSystemApp" to ((app.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                )
            }
            .sortedBy { (it["appName"] as String).lowercase() }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun saveEmbedding(name: String, embedding: FloatArray) {
        // Load existing
        val embeddingsJson = prefs.getString("face_embeddings", "[]") ?: "[]"
        val namesJson = prefs.getString("face_names", "[]") ?: "[]"

        val embeddingsType = object : TypeToken<MutableList<List<Float>>>() {}.type
        val namesType = object : TypeToken<MutableList<String>>() {}.type

        val embeddings: MutableList<List<Float>> = gson.fromJson(embeddingsJson, embeddingsType) ?: mutableListOf()
        val names: MutableList<String> = gson.fromJson(namesJson, namesType) ?: mutableListOf()

        embeddings.add(embedding.toList())
        names.add(name)

        prefs.edit()
            .putString("face_embeddings", gson.toJson(embeddings))
            .putString("face_names", gson.toJson(names))
            .apply()

        Log.d(TAG, "Saved face embedding for $name (total: ${embeddings.size})")
    }

    private fun loadRegisteredEmbeddings(): List<FloatArray> {
        val json = prefs.getString("face_embeddings", "[]") ?: "[]"
        val type = object : TypeToken<List<List<Float>>>() {}.type
        val raw: List<List<Float>> = gson.fromJson(json, type) ?: emptyList()
        return raw.map { it.toFloatArray() }
    }

    private fun getRegisteredFaceNames(): List<String> {
        val json = prefs.getString("face_names", "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /**
     * Decode JPEG bytes into a correctly-oriented Bitmap.
     * BitmapFactory.decodeByteArray does NOT apply EXIF rotation —
     * front-camera images are typically rotated 90° or 270°, causing
     * ML Kit to miss the face. This method reads EXIF orientation
     * and physically rotates the bitmap so it is always upright.
     */
    private fun decodeBitmapWithExifRotation(bytes: ByteArray): Bitmap? {
        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val rotationDegrees = try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> 0  // handled below
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> 0
                else -> 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read EXIF, assuming rotation=0", e)
            0
        }

        Log.d(TAG, "EXIF rotation: $rotationDegrees°, raw bitmap: ${rawBitmap.width}x${rawBitmap.height}")

        if (rotationDegrees == 0) return rawBitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(
            rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
        )
        if (rotated != rawBitmap) rawBitmap.recycle()
        return rotated
    }

    private fun deleteFace(index: Int) {
        val embeddingsJson = prefs.getString("face_embeddings", "[]") ?: "[]"
        val namesJson = prefs.getString("face_names", "[]") ?: "[]"

        val embeddingsType = object : TypeToken<MutableList<List<Float>>>() {}.type
        val namesType = object : TypeToken<MutableList<String>>() {}.type

        val embeddings: MutableList<List<Float>> = gson.fromJson(embeddingsJson, embeddingsType) ?: mutableListOf()
        val names: MutableList<String> = gson.fromJson(namesJson, namesType) ?: mutableListOf()

        if (index in 0 until embeddings.size) {
            embeddings.removeAt(index)
            names.removeAt(index)
            prefs.edit()
                .putString("face_embeddings", gson.toJson(embeddings))
                .putString("face_names", gson.toJson(names))
                .apply()
        }
    }
}
