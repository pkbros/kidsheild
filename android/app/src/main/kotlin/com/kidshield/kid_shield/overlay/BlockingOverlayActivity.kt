package com.kidshield.kid_shield.overlay

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kidshield.kid_shield.R
import com.kidshield.kid_shield.services.BuddyContentEngine
import com.kidshield.kid_shield.services.FaceRecognitionEngine
import com.kidshield.kid_shield.services.KidShieldAccessibilityService
import com.kidshield.kid_shield.services.UsageTrackingEngine
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BlockingOverlayActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BlockOverlay"
        private const val PREFS_NAME = "kid_shield_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_FACE_EMBEDDINGS = "face_embeddings"
        private const val KEY_VERIFICATION_SESSIONS = "verification_sessions"
        private const val KEY_REVERIFICATION_INTERVAL = "reverification_interval_seconds"
        private const val MAX_FACE_ATTEMPTS = 3
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var faceEngine: FaceRecognitionEngine
    private lateinit var buddyEngine: BuddyContentEngine
    private lateinit var cameraExecutor: ExecutorService
    private val gson = Gson()

    private var blockedPackage: String = ""
    private var faceAttempts = 0
    private var imageCapture: ImageCapture? = null
    private var isMascotMode = true
    private var overlayMode = BuddyContentEngine.MODE_VIDEO

    // Views
    private lateinit var appIconView: ImageView
    private lateinit var appNameView: TextView
    private lateinit var messageView: TextView
    private lateinit var verifyButton: Button
    private lateinit var goBackButton: Button
    private lateinit var pinButton: Button
    private lateinit var cameraPreview: PreviewView
    private lateinit var cameraContainer: LinearLayout
    private lateinit var pinContainer: LinearLayout
    private lateinit var pinInput: EditText
    private lateinit var pinSubmitButton: Button
    private lateinit var statusText: TextView

    // Buddy views
    private lateinit var buddyImageView: ImageView
    private lateinit var buddySpeechBubble: TextView

    // Video views
    private var mediaPlayer: MediaPlayer? = null
    private var videoTextureView: TextureView? = null
    private var videoBuddyBubble: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        faceEngine = FaceRecognitionEngine(this)
        buddyEngine = BuddyContentEngine(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        overlayMode = buddyEngine.getOverlayMode()
        isMascotMode = overlayMode != BuddyContentEngine.MODE_CLASSIC

        when (overlayMode) {
            BuddyContentEngine.MODE_VIDEO -> buildVideoUI()
            BuddyContentEngine.MODE_BUDDY -> buildBuddyUI()
            else -> buildClassicUI()
        }

        blockedPackage = intent.getStringExtra("blocked_package") ?: ""
        Log.d(TAG, "Overlay launched for: $blockedPackage (mascot: $isMascotMode)")

        setupAppInfo()
        setupButtons()
    }

    // ─── Helper: dp to px ───
    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()
    }

    // ─── Video Overlay UI (V1.5b) ───

    private fun buildVideoUI() {
        val buddyContent = buddyEngine.getNextContent()

        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
        }

        // ── Layer 1: Full-screen video TextureView ──
        videoTextureView = TextureView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(videoTextureView)

        videoTextureView!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                initVideoPlayer(Surface(surface))
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                releaseVideoPlayer()
                return true
            }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        // ── Layer 2: Top gradient scrim + app info ──
        val topScrim = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(40), dp(16), dp(16))
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xBB000000.toInt(), 0x00000000)
            )
            background = gradient
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
            layoutParams = lp
        }

        appIconView = ImageView(this).apply {
            val lp = LinearLayout.LayoutParams(dp(32), dp(32))
            lp.marginEnd = dp(10)
            layoutParams = lp
        }
        topScrim.addView(appIconView)

        val appTextCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        appNameView = TextView(this).apply {
            textSize = 15f
            setTextColor(0xDDFFFFFF.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        appTextCol.addView(appNameView)

        messageView = TextView(this).apply {
            text = "This app needs a grown-up's OK"
            textSize = 12f
            setTextColor(0x99FFFFFF.toInt())
        }
        appTextCol.addView(messageView)
        topScrim.addView(appTextCol)
        root.addView(topScrim)

        // ── Layer 3: Status text (floats above bottom section when needed) ──
        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFFFF8A65.toInt())
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(16), dp(8), dp(16), dp(8))
            val statusBg = GradientDrawable().apply {
                setColor(0xCC000000.toInt())
                cornerRadius = dp(12).toFloat()
            }
            background = statusBg
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
            lp.bottomMargin = dp(150)
            layoutParams = lp
        }
        root.addView(statusText)

        // ── Layer 5: Camera preview container (overlaid when active) ──
        cameraContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            layoutParams = lp
        }

        cameraPreview = PreviewView(this).apply {
            val lp = LinearLayout.LayoutParams(dp(200), dp(200))
            lp.gravity = Gravity.CENTER
            layoutParams = lp
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        cameraContainer.addView(cameraPreview)
        root.addView(cameraContainer)

        // ── Layer 6: PIN container (overlaid when active) ──
        pinContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(32), dp(20), dp(32), dp(20))
            val pinBg = GradientDrawable().apply {
                setColor(0xDD000000.toInt())
                cornerRadius = dp(20).toFloat()
            }
            background = pinBg
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            lp.marginStart = dp(32)
            lp.marginEnd = dp(32)
            layoutParams = lp
        }

        val pinLabel = TextView(this).apply {
            text = "Enter Parent PIN"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(12)
            layoutParams = lp
        }
        pinContainer.addView(pinLabel)

        pinInput = EditText(this).apply {
            hint = "PIN (6+ digits)"
            setHintTextColor(0x77FFFFFF.toInt())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            val bgShape = GradientDrawable().apply {
                setColor(0x33FFFFFF.toInt())
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), 0x55FFFFFF.toInt())
            }
            background = bgShape
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val lp = LinearLayout.LayoutParams(dp(220), LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = Gravity.CENTER
            lp.bottomMargin = dp(12)
            layoutParams = lp
        }
        pinContainer.addView(pinInput)

        pinSubmitButton = Button(this).apply {
            text = "Submit PIN"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            val bgShape = GradientDrawable().apply {
                setColor(0xFF66BB6A.toInt())
                cornerRadius = dp(24).toFloat()
            }
            background = bgShape
            setPadding(dp(24), dp(10), dp(24), dp(10))
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            layoutParams = lp
        }
        pinContainer.addView(pinSubmitButton)
        root.addView(pinContainer)

        // ── Layer 7: Bottom gradient scrim with motivating text + small buttons ──
        val bottomSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(48), dp(16), dp(20))
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(0xCC000000.toInt(), 0x00000000)
            )
            background = gradient
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
            layoutParams = lp
        }

        // Motivating text bubble (inside bottom scrim)
        videoBuddyBubble = TextView(this).apply {
            text = buddyContent.message
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            val bubbleBg = GradientDrawable().apply {
                setColor(0x55000000.toInt())
                cornerRadius = dp(16).toFloat()
            }
            background = bubbleBg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(10)
            layoutParams = lp
        }
        bottomSection.addView(videoBuddyBubble)

        // Row: Verify + PIN + Go Back (compact, side by side)
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        verifyButton = Button(this).apply {
            text = "\uD83D\uDC4B Verify"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            val bgShape = GradientDrawable().apply {
                setColor(0x66FFFFFF.toInt())
                cornerRadius = dp(20).toFloat()
            }
            background = bgShape
            setPadding(dp(16), dp(8), dp(16), dp(8))
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8)
            layoutParams = lp
        }
        bottomRow.addView(verifyButton)

        pinButton = Button(this).apply {
            text = "PIN"
            textSize = 12f
            setTextColor(0xBBFFFFFF.toInt())
            visibility = View.GONE
            val bgShape = GradientDrawable().apply {
                setColor(0x00000000)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0x55FFFFFF.toInt())
            }
            background = bgShape
            setPadding(dp(14), dp(6), dp(14), dp(6))
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8)
            layoutParams = lp
        }
        bottomRow.addView(pinButton)

        goBackButton = Button(this).apply {
            text = "← Back"
            textSize = 12f
            setTextColor(0xBBFFFFFF.toInt())
            val bgShape = GradientDrawable().apply {
                setColor(0x00000000)
            }
            background = bgShape
            isAllCaps = false
        }
        bottomRow.addView(goBackButton)

        bottomSection.addView(bottomRow)
        root.addView(bottomSection)

        // Initialize unused buddy views to avoid lateinit crashes
        buddyImageView = ImageView(this).apply { visibility = View.GONE }
        buddySpeechBubble = TextView(this).apply { visibility = View.GONE }

        setContentView(root)
    }

    // ─── Video Player Helpers ───

    private fun pickRandomVideo(): Int {
        val videos = intArrayOf(R.raw.buddy_video_1, R.raw.buddy_video_2, R.raw.buddy_video_3)
        val lastPlayed = prefs.getInt("last_buddy_video", -1)
        var pick: Int
        do {
            pick = videos.indices.random()
        } while (pick == lastPlayed && videos.size > 1)
        prefs.edit().putInt("last_buddy_video", pick).apply()
        return videos[pick]
    }

    private fun initVideoPlayer(surface: Surface) {
        try {
            releaseVideoPlayer()
            val videoResId = pickRandomVideo()
            val mp = MediaPlayer()
            val afd = resources.openRawResourceFd(videoResId)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setSurface(surface)
            mp.isLooping = true
            // Play with original audio (videos have motivating voices)
            mp.setVolume(1f, 1f)
            mp.setOnPreparedListener { player ->
                // Scale video to fill TextureView (center-crop style)
                adjustVideoScale(player)
                player.start()
            }
            mp.prepareAsync()
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e(TAG, "Video player init failed", e)
        }
    }

    private fun adjustVideoScale(mp: MediaPlayer) {
        val tv = videoTextureView ?: return
        val videoWidth = mp.videoWidth.toFloat()
        val videoHeight = mp.videoHeight.toFloat()
        val viewWidth = tv.width.toFloat()
        val viewHeight = tv.height.toFloat()

        if (videoWidth == 0f || videoHeight == 0f || viewWidth == 0f || viewHeight == 0f) return

        val scaleX: Float
        val scaleY: Float
        // Center-crop: scale so video fills entire view
        val videoAspect = videoWidth / videoHeight
        val viewAspect = viewWidth / viewHeight
        if (videoAspect > viewAspect) {
            // Video is wider — scale by height, crop sides
            scaleY = 1f
            scaleX = (videoAspect / viewAspect)
        } else {
            // Video is taller — scale by width, crop top/bottom
            scaleX = 1f
            scaleY = (viewAspect / videoAspect)
        }

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        tv.setTransform(matrix)
    }

    private fun releaseVideoPlayer() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing video player", e)
        }
        mediaPlayer = null
    }

    private fun pauseVideo() {
        try {
            mediaPlayer?.let { if (it.isPlaying) it.pause() }
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing video", e)
        }
    }

    private fun resumeVideo() {
        try {
            mediaPlayer?.let { if (!it.isPlaying) it.start() }
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming video", e)
        }
    }

    // ─── Buddy Mascot Overlay UI (V1.5) ───

    private fun buildBuddyUI() {
        val buddyContent = buddyEngine.getNextContent()

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(0xFFFFF8E1.toInt()) // warm cream/yellow background
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }

        // ── Buddy character illustration ──
        buddyImageView = ImageView(this).apply {
            val lp = LinearLayout.LayoutParams(dp(160), dp(160))
            lp.gravity = Gravity.CENTER
            lp.bottomMargin = dp(8)
            layoutParams = lp
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        // Set buddy pose drawable
        val poseResId = when (buddyContent.pose) {
            "waving" -> R.drawable.buddy_waving
            "thinking" -> R.drawable.buddy_thinking
            "pointing" -> R.drawable.buddy_pointing
            "encouraging" -> R.drawable.buddy_encouraging
            else -> R.drawable.buddy_waving
        }
        buddyImageView.setImageResource(poseResId)
        mainLayout.addView(buddyImageView)

        // ── Speech bubble ──
        val speechBubbleContainer = FrameLayout(this).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(20)
            layoutParams = lp
        }

        buddySpeechBubble = TextView(this).apply {
            text = buddyContent.message
            textSize = 16f
            setTextColor(0xFF424242.toInt())
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setPadding(dp(20), dp(16), dp(20), dp(16))
            setBackgroundResource(R.drawable.speech_bubble_bg)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }
        speechBubbleContainer.addView(buddySpeechBubble)
        mainLayout.addView(speechBubbleContainer)

        // ── App info section ──
        val appInfoSection = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val bgShape = GradientDrawable().apply {
                setColor(0x15000000)
                cornerRadius = dp(12).toFloat()
            }
            background = bgShape
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(12)
            layoutParams = lp
        }

        appIconView = ImageView(this).apply {
            val lp = LinearLayout.LayoutParams(dp(40), dp(40))
            lp.marginEnd = dp(12)
            layoutParams = lp
        }
        appInfoSection.addView(appIconView)

        val appTextContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        appNameView = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFF424242.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        appTextContainer.addView(appNameView)

        messageView = TextView(this).apply {
            text = "This app needs a grown-up's OK"
            textSize = 13f
            setTextColor(0xFF9E9E9E.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        appTextContainer.addView(messageView)
        appInfoSection.addView(appTextContainer)
        mainLayout.addView(appInfoSection)

        // ── Status text ──
        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFFF7043.toInt())
            gravity = Gravity.CENTER
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(8)
            layoutParams = lp
        }
        mainLayout.addView(statusText)

        // ── Camera preview container ──
        cameraContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(12)
            layoutParams = lp
        }

        cameraPreview = PreviewView(this).apply {
            val lp = LinearLayout.LayoutParams(dp(200), dp(200))
            lp.gravity = Gravity.CENTER
            layoutParams = lp
            val clipShape = GradientDrawable().apply {
                cornerRadius = dp(100).toFloat()
            }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        cameraContainer.addView(cameraPreview)
        mainLayout.addView(cameraContainer)

        // ── PIN container ──
        pinContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(12)
            layoutParams = lp
        }

        val pinLabel = TextView(this).apply {
            text = "Enter Parent PIN"
            textSize = 15f
            setTextColor(0xFF424242.toInt())
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(12)
            layoutParams = lp
        }
        pinContainer.addView(pinLabel)

        pinInput = EditText(this).apply {
            hint = "PIN (6+ digits)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
            textSize = 22f
            val bgShape = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(12).toFloat()
                setStroke(dp(2), 0xFFE0E0E0.toInt())
            }
            background = bgShape
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val lp = LinearLayout.LayoutParams(dp(220), LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = Gravity.CENTER
            lp.bottomMargin = dp(12)
            layoutParams = lp
        }
        pinContainer.addView(pinInput)

        pinSubmitButton = Button(this).apply {
            text = "Submit PIN"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            val bgShape = GradientDrawable().apply {
                setColor(0xFF66BB6A.toInt())
                cornerRadius = dp(24).toFloat()
            }
            background = bgShape
            setPadding(dp(24), dp(10), dp(24), dp(10))
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            layoutParams = lp
        }
        pinContainer.addView(pinSubmitButton)
        mainLayout.addView(pinContainer)

        // ── Verify button (primary action, friendly styled) ──
        verifyButton = Button(this).apply {
            text = "👋 Ask a Grown-Up to Look"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            val bgShape = GradientDrawable().apply {
                setColor(0xFF42A5F5.toInt())
                cornerRadius = dp(28).toFloat()
            }
            background = bgShape
            setPadding(dp(20), dp(14), dp(20), dp(14))
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(10)
            layoutParams = lp
        }
        mainLayout.addView(verifyButton)

        // ── PIN button ──
        pinButton = Button(this).apply {
            text = "Use PIN Instead"
            textSize = 13f
            setTextColor(0xFF78909C.toInt())
            visibility = View.GONE
            val bgShape = GradientDrawable().apply {
                setColor(0x00000000)
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), 0xFFB0BEC5.toInt())
            }
            background = bgShape
            setPadding(dp(16), dp(8), dp(16), dp(8))
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            lp.bottomMargin = dp(10)
            layoutParams = lp
        }
        mainLayout.addView(pinButton)

        // ── Go back button ──
        goBackButton = Button(this).apply {
            text = "← Go Back Home"
            textSize = 13f
            setTextColor(0xFF78909C.toInt())
            val bgShape = GradientDrawable().apply {
                setColor(0x00000000)
            }
            background = bgShape
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER
            layoutParams = lp
        }
        mainLayout.addView(goBackButton)

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    // ─── Classic Overlay UI (fallback when mascot disabled) ───

    private fun buildClassicUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFFF5F5F5.toInt())
            setPadding(48, 64, 48, 64)
        }

        // App icon
        appIconView = ImageView(this).apply {
            val lp = LinearLayout.LayoutParams(128, 128)
            lp.bottomMargin = 24
            layoutParams = lp
        }
        mainLayout.addView(appIconView)

        // App name
        appNameView = TextView(this).apply {
            textSize = 22f
            setTextColor(0xFF212121.toInt())
            gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 16
            layoutParams = lp
        }
        mainLayout.addView(appNameView)

        // Message
        messageView = TextView(this).apply {
            text = "This app is restricted.\nParent verification required."
            textSize = 16f
            setTextColor(0xFF757575.toInt())
            gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 48
            layoutParams = lp
        }
        mainLayout.addView(messageView)

        // Status text
        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFFF5722.toInt())
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 16
            layoutParams = lp
        }
        mainLayout.addView(statusText)

        // Camera preview container
        cameraContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            lp.bottomMargin = 24
            layoutParams = lp
        }

        cameraPreview = PreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(480, 480).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        cameraContainer.addView(cameraPreview)
        mainLayout.addView(cameraContainer)

        // PIN container
        pinContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 24
            layoutParams = lp
        }

        val pinLabel = TextView(this).apply {
            text = "Enter Parent PIN"
            textSize = 16f
            setTextColor(0xFF424242.toInt())
            gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 16
            layoutParams = lp
        }
        pinContainer.addView(pinLabel)

        pinInput = EditText(this).apply {
            hint = "PIN (6+ digits)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = android.view.Gravity.CENTER
            textSize = 24f
            val lp = LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.gravity = android.view.Gravity.CENTER
            lp.bottomMargin = 16
            layoutParams = lp
        }
        pinContainer.addView(pinInput)

        pinSubmitButton = Button(this).apply {
            text = "Submit PIN"
            textSize = 16f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = android.view.Gravity.CENTER
            layoutParams = lp
        }
        pinContainer.addView(pinSubmitButton)
        mainLayout.addView(pinContainer)

        // Verify button
        verifyButton = Button(this).apply {
            text = "🔐 Verify with Face"
            textSize = 18f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 16
            layoutParams = lp
        }
        mainLayout.addView(verifyButton)

        // Use PIN button
        pinButton = Button(this).apply {
            text = "Use PIN Instead"
            textSize = 14f
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 16
            lp.gravity = android.view.Gravity.CENTER
            layoutParams = lp
        }
        mainLayout.addView(pinButton)

        // Go back button
        goBackButton = Button(this).apply {
            text = "← Go Back Home"
            textSize = 14f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = android.view.Gravity.CENTER
            layoutParams = lp
        }
        mainLayout.addView(goBackButton)

        // Initialize buddy views to avoid lateinit crashes (hidden in classic mode)
        buddyImageView = ImageView(this).apply { visibility = View.GONE }
        buddySpeechBubble = TextView(this).apply { visibility = View.GONE }

        setContentView(mainLayout)
    }

    private fun setupAppInfo() {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(blockedPackage, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val appIcon = pm.getApplicationIcon(appInfo)

            appNameView.text = appName
            appIconView.setImageDrawable(appIcon)
        } catch (e: PackageManager.NameNotFoundException) {
            appNameView.text = blockedPackage
        }
    }

    private fun setupButtons() {
        verifyButton.setOnClickListener {
            startFaceVerification()
        }

        goBackButton.setOnClickListener {
            goHome()
        }

        pinButton.setOnClickListener {
            showPinEntry()
        }

        pinSubmitButton.setOnClickListener {
            verifyPin()
        }
    }

    private fun startFaceVerification() {
        faceAttempts++
        verifyButton.visibility = View.GONE
        cameraContainer.visibility = View.VISIBLE
        statusText.visibility = View.VISIBLE

        // In mascot/video mode, hide/pause content while camera is active
        if (overlayMode == BuddyContentEngine.MODE_VIDEO) {
            pauseVideo()
            videoBuddyBubble?.visibility = View.GONE
            statusText.text = "Hold still… looking for a grown-up! (Attempt $faceAttempts/$MAX_FACE_ATTEMPTS)"
        } else if (overlayMode == BuddyContentEngine.MODE_BUDDY) {
            buddyImageView.visibility = View.GONE
            buddySpeechBubble.visibility = View.GONE
            statusText.text = "Hold still… looking for a grown-up! (Attempt $faceAttempts/$MAX_FACE_ATTEMPTS)"
        } else {
            statusText.text = "Looking for face... (Attempt $faceAttempts/$MAX_FACE_ATTEMPTS)"
        }

        startCamera()

        // Auto-capture after 2 seconds
        object : CountDownTimer(2500, 2500) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                captureAndVerify()
            }
        }.start()
    }

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture = future
        future.addListener(Runnable {
            try {
                val cameraProvider: ProcessCameraProvider = future.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this@BlockingOverlayActivity, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera start failed", e)
                onVerificationFailed("Camera error")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndVerify() {
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val bitmap = imageProxyToBitmap(imageProxy, rotation)
                    imageProxy.close()

                    if (bitmap != null) {
                        performFaceVerification(bitmap)
                    } else {
                        onVerificationFailed("Could not capture image")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exception)
                    onVerificationFailed("Capture failed")
                }
            }
        )
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy, rotationDegrees: Int): Bitmap? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        Log.d(TAG, "ImageProxy rotation: $rotationDegrees°, bitmap: ${rawBitmap.width}x${rawBitmap.height}")

        if (rotationDegrees == 0) return rawBitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(
            rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
        )
        if (rotated != rawBitmap) rawBitmap.recycle()
        return rotated
    }

    private fun performFaceVerification(bitmap: Bitmap) {
        val registeredEmbeddings = loadRegisteredEmbeddings()
        if (registeredEmbeddings.isEmpty()) {
            onVerificationFailed("No faces registered")
            return
        }

        faceEngine.processFrame(bitmap) { embedding ->
            runOnUiThread {
                if (embedding != null) {
                    val (matchIndex, similarity) = faceEngine.matchAgainstRegistered(
                        embedding, registeredEmbeddings
                    )
                    if (matchIndex >= 0) {
                        Log.d(TAG, "Face matched parent #$matchIndex (similarity: $similarity)")
                        lastVerificationMethod = "face_verified"
                        onVerificationSuccess()
                    } else {
                        Log.d(TAG, "Face did not match (best similarity: $similarity)")
                        onVerificationFailed("Face not recognized")
                    }
                } else {
                    onVerificationFailed("No face detected")
                }
            }
        }
    }

    private fun loadRegisteredEmbeddings(): List<FloatArray> {
        val json = prefs.getString(KEY_FACE_EMBEDDINGS, "[]") ?: "[]"
        val type = object : TypeToken<List<List<Float>>>() {}.type
        val raw: List<List<Float>> = gson.fromJson(json, type)
        return raw.map { it.toFloatArray() }
    }

    /** Track which verification method succeeded (set before calling this) */
    private var lastVerificationMethod: String = "face_verified"

    private fun onVerificationSuccess() {
        // Record verification timestamp
        recordVerification(blockedPackage)

        // Record outcome in usage analytics
        try {
            val tracker = UsageTrackingEngine.getInstance(this)
            tracker.recordBlockOutcome(lastVerificationMethod)
            tracker.onOverlayDismissed()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record block outcome", e)
        }

        // Stop camera
        stopCamera()

        Toast.makeText(this, "✓ Access granted", Toast.LENGTH_SHORT).show()

        // Notify accessibility service that overlay is closing (don't reset foreground tracking)
        KidShieldAccessibilityService.instance?.onOverlayDismissed()

        // Close overlay
        finish()
    }

    private fun onVerificationFailed(reason: String) {
        statusText.text = "$reason — Attempt $faceAttempts/$MAX_FACE_ATTEMPTS"

        if (faceAttempts >= MAX_FACE_ATTEMPTS) {
            // Show PIN option
            stopCamera()
            cameraContainer.visibility = View.GONE
            verifyButton.visibility = View.VISIBLE
            pinButton.visibility = View.VISIBLE
            statusText.text = if (isMascotMode) {
                "Hmm, I couldn't recognize the face. Try PIN!"
            } else {
                "Face verification failed. Use PIN to unlock."
            }
        } else {
            // Allow retry
            verifyButton.visibility = View.VISIBLE
            verifyButton.text = if (isMascotMode) "👋 Try Again" else "🔐 Try Again"
            cameraContainer.visibility = View.GONE
        }

        // Resume video/buddy content when camera is dismissed
        if (overlayMode == BuddyContentEngine.MODE_VIDEO) {
            resumeVideo()
            videoBuddyBubble?.visibility = View.VISIBLE
        } else if (overlayMode == BuddyContentEngine.MODE_BUDDY) {
            buddyImageView.visibility = View.VISIBLE
            buddySpeechBubble.visibility = View.VISIBLE
        }
    }

    private fun showPinEntry() {
        verifyButton.visibility = View.GONE
        pinButton.visibility = View.GONE
        cameraContainer.visibility = View.GONE
        pinContainer.visibility = View.VISIBLE
        statusText.visibility = View.GONE
    }

    private fun verifyPin() {
        val enteredPin = pinInput.text.toString()
        if (enteredPin.length < 6) {
            Toast.makeText(this, "PIN must be at least 6 digits", Toast.LENGTH_SHORT).show()
            return
        }

        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        val enteredHash = hashPin(enteredPin)

        if (enteredHash == storedHash) {
            lastVerificationMethod = "pin_verified"
            onVerificationSuccess()
        } else {
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            pinInput.text.clear()
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun recordVerification(packageName: String) {
        val sessionsJson = prefs.getString(KEY_VERIFICATION_SESSIONS, "{}") ?: "{}"
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        val sessions: MutableMap<String, Long> = gson.fromJson(sessionsJson, type)
        sessions[packageName] = System.currentTimeMillis()
        prefs.edit().putString(KEY_VERIFICATION_SESSIONS, gson.toJson(sessions)).apply()
    }

    private fun stopCamera() {
        try {
            val future = cameraProviderFuture ?: return
            if (future.isDone) {
                future.get().unbindAll()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
    }

    private fun goHome() {
        // Record navigated_away outcome in usage analytics
        try {
            val tracker = UsageTrackingEngine.getInstance(this)
            tracker.recordBlockOutcome("navigated_away")
            tracker.onOverlayDismissed()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to record navigated_away outcome", e)
        }

        // Navigate to home screen instead of back to blocked app
        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        KidShieldAccessibilityService.instance?.onOverlayDismissed()
        KidShieldAccessibilityService.instance?.resetLastDetected()
        finish()
    }

    // Prevent back button from dismissing overlay
    override fun onBackPressed() {
        // Do nothing — overlay cannot be dismissed
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Block back and recent keys
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        // If the overlay loses focus (e.g., user presses Home), go to home
        // The blocked app should not be accessible
    }

    override fun onDestroy() {
        stopCamera()
        releaseVideoPlayer()
        cameraExecutor.shutdown()
        faceEngine.close()

        // Safety net: if onOverlayDismissed() wasn't called (e.g., system killed us),
        // reset the flag so the accessibility service isn't permanently blocked.
        KidShieldAccessibilityService.instance?.onOverlayDismissed()

        super.onDestroy()
    }
}
