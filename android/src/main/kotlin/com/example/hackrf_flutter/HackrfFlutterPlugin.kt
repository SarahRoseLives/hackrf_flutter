package com.example.hackrf_flutter

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

import com.mantz_it.hackrf_android.Hackrf
import com.mantz_it.hackrf_android.HackrfCallbackInterface

class HackrfFlutterPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var hackrf: Hackrf? = null
    private var pendingInitResult: Result? = null
    private var txQueue: ArrayBlockingQueue<ByteArray>? = null
    private var isTransmitting = false

    private val hackrfCallback = object : HackrfCallbackInterface {
        override fun onHackrfReady(hackrfInstance: Hackrf) {
            this@HackrfFlutterPlugin.hackrf = hackrfInstance
            pendingInitResult?.success(true)
            pendingInitResult = null
        }

        override fun onHackrfError(message: String) {
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
        // Run hardware calls on a background thread to not block the UI
        thread(start = true) {
            try {
                when (call.method) {
                    "init" -> {
                        if (pendingInitResult != null) {
                            result.error("ALREADY_INITIALIZING", "An initialization process is already underway.", null)
                            return@thread
                        }
                        pendingInitResult = result
                        val context = pluginBinding?.applicationContext ?: run {
                            result.error("CONTEXT_UNAVAILABLE", "Android context is not available.", null)
                            return@thread
                        }
                        Hackrf.initHackrf(context, hackrfCallback, 10)
                    }
                    "getBoardId" -> {
                        val boardId = hackrf?.boardID
                        result.success(boardId?.toInt())
                    }
                    "setFrequency" -> {
                        val freq = call.argument<Int>("freq")?.toLong() ?: 0L
                        val success = hackrf?.setFrequency(freq)
                        result.success(success)
                    }
                    "setSampleRate" -> {
                        val rate = call.argument<Int>("rate") ?: 0
                        // For NTSC, a sample rate of 10 MSPS is common. Divider is 1 for that.
                        val success = hackrf?.setSampleRate(rate, 1)
                        result.success(success)
                    }
                    "setTxVgaGain" -> {
                        val gain = call.argument<Int>("gain") ?: 0
                        val success = hackrf?.setTxVGAGain(gain)
                        result.success(success)
                    }
                    "startTx" -> {
                        txQueue = hackrf?.startTX()
                        isTransmitting = (txQueue != null)
                        result.success(isTransmitting)
                    }
                    "stopTx" -> {
                        hackrf?.stop()
                        isTransmitting = false
                        txQueue = null
                        result.success(true)
                    }
                    "sendData" -> {
                        val data = call.argument<ByteArray>("data")
                        if (isTransmitting && data != null) {
                            txQueue?.put(data)
                            result.success(null)
                        } else {
                            result.error("TX_ERROR", "Not transmitting or data is null.", null)
                        }
                    }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                result.error("NATIVE_ERROR", e.message, e.stackTraceToString())
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        hackrf?.stop()
        hackrf = null
        pluginBinding = null
        pendingInitResult = null
    }
}