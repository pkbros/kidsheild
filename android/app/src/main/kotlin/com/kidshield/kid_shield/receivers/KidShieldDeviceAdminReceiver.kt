package com.kidshield.kid_shield.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class KidShieldDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "KidShieldAdmin"
        private const val PREFS_NAME = "kid_shield_prefs"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device admin enabled")
        Toast.makeText(context, "KidShield: Uninstall protection active", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device admin disabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Launch PIN verification activity
        Log.d(TAG, "Device admin disable requested - launching PIN challenge")
        val challengeIntent = Intent(context, com.kidshield.kid_shield.overlay.AdminPinChallengeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(challengeIntent)

        return "WARNING: Disabling device admin requires parent PIN verification. If you did not authorize this, the app will re-enable protection."
    }
}
