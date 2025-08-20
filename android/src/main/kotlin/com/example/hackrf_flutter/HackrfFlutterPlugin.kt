package com.example.hackrf_flutter

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

// Import the required classes from your AAR file
import com.mantz_it.hackrf_android.Hackrf
import com.mantz_it.hackrf_android.HackrfCallbackInterface

/** HackrfFlutterPlugin */
class HackrfFlutterPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var hackrf: Hackrf? = null

    // This variable will hold the Flutter result callback while we wait for the native library
    private var pendingInitResult: Result? = null

    // This callback structure correctly matches your HackrfCallbackInterface.java
    private val hackrfCallback = object : HackrfCallbackInterface {
        override fun onHackrfReady(hackrfInstance: Hackrf) {
            // The library is ready! It gave us the Hackrf instance.
            this@HackrfFlutterPlugin.hackrf = hackrfInstance
            // Signal success back to Flutter
            pendingInitResult?.success(true)
            pendingInitResult = null
        }

        override fun onHackrfError(message: String) {
            // The library failed to connect. Send an error back to Flutter.
            pendingInitResult?.error("HACKRF_ERROR", message, null)
            pendingInitResult = null
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.pluginBinding = flutterPluginBinding
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "hackrf_flutter_channel")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "init" -> {
                if (pendingInitResult != null) {
                    result.error("ALREADY_INITIALIZING", "An initialization process is already underway.", null)
                    return
                }
                pendingInitResult = result
                val context = pluginBinding?.applicationContext
                if (context == null) {
                    result.error("CONTEXT_UNAVAILABLE", "Android context is not available.", null)
                    return
                }

                try {
                    // CORRECT: Call the static 'initHackrf' method to start the process.
                    // Using a default queue size of 10.
                    Hackrf.initHackrf(context, hackrfCallback, 10)
                } catch (e: Exception) {
                    pendingInitResult?.error("INIT_ERROR", "Failed to call static Hackrf.initHackrf()", e.message)
                    pendingInitResult = null
                }
            }
            "getBoardId" -> {
                if (hackrf == null) {
                    result.error("NOT_INITIALIZED", "Hackrf is not initialized. Call init() first.", null)
                    return
                }
                try {
                    // CORRECT: Call the instance 'getBoardID()' method.
                    val boardId = hackrf?.boardID
                    // Convert the byte to an Int for Dart, as it's easier to work with.
                    result.success(boardId?.toInt())
                } catch (e: Exception) {
                    result.error("BOARD_ID_ERROR", "Failed to get board ID", e.message)
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        hackrf = null
        pluginBinding = null
        pendingInitResult = null
    }
}