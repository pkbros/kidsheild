package com.kidshield.kid_shield.overlay

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kidshield.kid_shield.receivers.KidShieldDeviceAdminReceiver
import java.security.MessageDigest

/**
 * Full-screen PIN challenge that appears when someone tries to disable
 * device admin (which would allow uninstalling KidShield).
 *
 * If the correct PIN is entered, the activity closes and lets the system
 * proceed. If the PIN is not verified within the timeout, or the user
 * dismisses the screen, we re-enable device admin and re-lock.
 */
class AdminPinChallengeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AdminPinChallenge"
        private const val PREFS_NAME = "kid_shield_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val TIMEOUT_MS = 30_000L  // 30 seconds to enter PIN
    }

    private lateinit var prefs: SharedPreferences
    private var pinVerified = false
    private var timer: CountDownTimer? = null

    private lateinit var pinInput: EditText
    private lateinit var statusText: TextView
    private lateinit var timerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        buildUI()
        startTimeout()
        Log.d(TAG, "Admin PIN challenge launched")
    }

    private fun buildUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFFF44336.toInt())  // Red background = danger
            setPadding(64, 100, 64, 100)
        }

        // Warning icon
        val warningText = TextView(this).apply {
            text = "\u26A0\uFE0F"  // Warning emoji
            textSize = 48f
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 24
            layoutParams = lp
        }
        mainLayout.addView(warningText)

        // Title
        val titleText = TextView(this).apply {
            text = "Parent Verification Required"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 16
            layoutParams = lp
        }
        mainLayout.addView(titleText)

        // Message
        val messageText = TextView(this).apply {
            text = "Someone is trying to disable KidShield's\nuninstall protection.\n\nEnter the parent PIN to authorize this action."
            textSize = 15f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 32
            layoutParams = lp
        }
        mainLayout.addView(messageText)

        // Timer countdown
        timerText = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(0xAAFFFFFF.toInt())
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 24
            layoutParams = lp
        }
        mainLayout.addView(timerText)

        // PIN input
        pinInput = EditText(this).apply {
            hint = "Enter PIN"
            setHintTextColor(0x88FFFFFF.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
            textSize = 28f
            setBackgroundColor(0x33FFFFFF.toInt())
            setPadding(24, 16, 24, 16)
            val lp = LinearLayout.LayoutParams(350, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = Gravity.CENTER
            lp.bottomMargin = 24
            layoutParams = lp
        }
        mainLayout.addView(pinInput)

        // Status text
        statusText = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(0xFFFFEB3B.toInt())  // Yellow for errors
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 24
            layoutParams = lp
        }
        mainLayout.addView(statusText)

        // Submit button
        val submitButton = Button(this).apply {
            text = "Verify PIN"
            textSize = 16f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            lp.bottomMargin = 16
            layoutParams = lp
        }
        submitButton.setOnClickListener { verifyPin() }
        mainLayout.addView(submitButton)

        // Cancel button
        val cancelButton = Button(this).apply {
            text = "Cancel (keep protection)"
            textSize = 13f
            setBackgroundColor(0x00000000)
            setTextColor(0xCCFFFFFF.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            layoutParams = lp
        }
        cancelButton.setOnClickListener {
            pinVerified = false
            finish()
        }
        mainLayout.addView(cancelButton)

        setContentView(mainLayout)
    }

    private fun startTimeout() {
        timer = object : CountDownTimer(TIMEOUT_MS, 1000) {
            override fun onTick(remaining: Long) {
                val secs = remaining / 1000
                timerText.text = "Auto-cancel in ${secs}s"
            }

            override fun onFinish() {
                if (!pinVerified) {
                    Log.d(TAG, "Timeout - PIN not entered, closing challenge")
                    Toast.makeText(
                        this@AdminPinChallengeActivity,
                        "Timeout: device admin disable blocked",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }.start()
    }

    private fun verifyPin() {
        val entered = pinInput.text.toString()
        if (entered.length < 6) {
            statusText.text = "PIN must be at least 6 digits"
            return
        }

        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (storedHash == null) {
            // No PIN set - allow (shouldn't happen in practice)
            statusText.text = "No PIN configured - allowing"
            pinVerified = true
            timer?.cancel()
            finish()
            return
        }

        val enteredHash = hashPin(entered)
        if (enteredHash == storedHash) {
            pinVerified = true
            timer?.cancel()
            Toast.makeText(this, "PIN verified - admin disable authorized", Toast.LENGTH_LONG).show()
            Log.d(TAG, "PIN verified - allowing admin disable")
            finish()
        } else {
            statusText.text = "Incorrect PIN"
            pinInput.text.clear()
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    override fun onBackPressed() {
        // Block back button
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        timer?.cancel()

        // If PIN was NOT verified, re-enable device admin to prevent uninstall
        if (!pinVerified) {
            Log.d(TAG, "PIN not verified - attempting to keep admin active")
            reEnableAdminIfNeeded()
        }

        super.onDestroy()
    }

    private fun reEnableAdminIfNeeded() {
        try {
            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val componentName = ComponentName(this, KidShieldDeviceAdminReceiver::class.java)
            if (!dpm.isAdminActive(componentName)) {
                // Admin was already disabled, prompt to re-enable
                val intent = android.content.Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "KidShield needs device admin to prevent unauthorized uninstall."
                    )
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-enable admin", e)
        }
    }
}
