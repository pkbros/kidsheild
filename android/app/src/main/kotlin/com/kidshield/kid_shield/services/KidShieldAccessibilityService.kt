package com.kidshield.kid_shield.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Optional accelerator for foreground app detection.
 *
 * When the user grants Accessibility permission, this service provides
 * INSTANT (0ms) foreground app detection via TYPE_WINDOW_STATE_CHANGED,
 * complementing the primary UsageStatsManager polling (200-400ms latency).
 *
 * If Accessibility is NOT granted, the app still works via polling alone.
 * This makes the app Play Store compliant by default — Accessibility is
 * an optional power-user enhancement, not a requirement.
 *
 * All blocking logic is delegated to AppBlockingController (shared
 * single source of truth for overlay state, verification, etc.).
 */
class KidShieldAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KidShieldA11y"
        var instance: KidShieldAccessibilityService? = null
            private set
    }

    private lateinit var controller: AppBlockingController

    override fun onCreate() {
        super.onCreate()
        instance = this
        controller = AppBlockingController.getInstance(this)
        Log.d(TAG, "Accessibility Service created (optional accelerator)")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        Log.d(TAG, "Accessibility Service destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Delegate all detection logic to the shared controller
        controller.onForegroundAppDetected(packageName)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected (instant detection active)")
    }

    /**
     * Called by BlockingOverlayActivity when overlay is dismissed.
     * Delegates to shared controller.
     */
    fun onOverlayDismissed() {
        controller.onOverlayDismissed()
    }

    fun resetLastDetected() {
        controller.resetLastDetected()
    }

    /**
     * Health check called by the foreground service watchdog.
     * Delegates to shared controller.
     */
    fun healthCheck() {
        controller.healthCheck()
    }
}
