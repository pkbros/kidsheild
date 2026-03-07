package com.kidshield.kid_shield

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.kidshield.kid_shield.channels.PlatformChannelHandler

class MainActivity : FlutterActivity() {

    private lateinit var platformChannel: MethodChannel

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val handler = PlatformChannelHandler(applicationContext, this)
        platformChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            PlatformChannelHandler.CHANNEL_NAME
        )
        platformChannel.setMethodCallHandler(handler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRouteIntent()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRouteIntent()
    }

    private fun handleRouteIntent() {
        val route = intent?.getStringExtra("route")
        if (!route.isNullOrEmpty()) {
            // Send the route to Flutter via the platform channel
            val channel = MethodChannel(
                flutterEngine!!.dartExecutor.binaryMessenger,
                PlatformChannelHandler.CHANNEL_NAME
            )
            channel.invokeMethod("navigateToRoute", route)
            // Clear the extra so it doesn't re-trigger
            intent?.removeExtra("route")
        }
    }
}
