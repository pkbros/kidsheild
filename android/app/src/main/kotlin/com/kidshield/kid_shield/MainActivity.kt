package com.kidshield.kid_shield

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
}
