package com.kidshield.kid_shield.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.kidshield.kid_shield.services.KidShieldForegroundService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KidShieldBoot"
        private const val PREFS_NAME = "kid_shield_prefs"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d(TAG, "Boot completed — checking if service should start")

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isSetupComplete = prefs.getBoolean("is_setup_complete", false)
            val isProtectionEnabled = prefs.getBoolean("is_protection_enabled", false)

            if (isSetupComplete && isProtectionEnabled) {
                Log.d(TAG, "Starting foreground service after boot")
                val serviceIntent = Intent(context, KidShieldForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
