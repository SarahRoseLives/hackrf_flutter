package com.example.hackrf_flutter

import android.util.Log
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
    // Define a TAG for easy log filtering
    private val TAG = "HackrfPlugin"

    private lateinit var channel: MethodChannel
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var hackrf: Hackrf? = null
    private var pendingInitResult: Result? = null
    private var txQueue: ArrayBlockingQueue<ByteArray>? = null
    private var isTransmitting = false

    private val hackrfCallback = object : HackrfCallbackInterface {
        override fun onHackrfReady(hackrfInstance: Hackrf) {
            Log.d(TAG, "✅ onHackrfReady: Device is ready!")
            this@HackrfFlutterPlugin.hackrf = hackrfInstance
            pendingInitResult?.success(true)
            pendingInitResult = null
        }

        override fun onHackrfError(message: String) {
            Log.e(TAG, "❌ onHackrfError: $message")
            pendingInitResult?.error("HACKRF_ERROR", message, null)
            pendingInitResult = null
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.pluginBinding = flutterPluginBinding
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "hackrf_flutter_channel")
        channel.setMethodCallHandler(this)
        Log.d(TAG, "Plugin attached to engine.")
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.d(TAG, "methodCall: ${call.method}")
        thread(start = true) {
            try {
                when (call.method) {
                    "init" -> {
                        // ... (code is the same, but now surrounded by logs)
                        pendingInitResult = result
                        val context = pluginBinding?.applicationContext ?: run {
                            Log.e(TAG, "Context is unavailable.")
                            result.error("CONTEXT_UNAVAILABLE", "Android context is not available.", null)
                            return@thread
                        }
                        Hackrf.initHackrf(context, hackrfCallback, 10)
                        Log.d(TAG, "initHackrf called.")
                    }
                    "getBoardId" -> {
                        val boardId = hackrf?.boardID
                        Log.d(TAG, "getBoardId successful, ID: $boardId")
                        result.success(boardId?.toInt())
                    }
                    "setFrequency" -> {
                        val freq = call.argument<Int>("freq")?.toLong() ?: 0L
                        val success = hackrf?.setFrequency(freq)
                        Log.d(TAG, "setFrequency to $freq Hz, Success: $success")
                        result.success(success)
                    }
                    "setSampleRate" -> {
                        val rate = call.argument<Int>("rate") ?: 0
                        val success = hackrf?.setSampleRate(rate, 1)
                        Log.d(TAG, "setSampleRate to $rate Hz, Success: $success")
                        result.success(success)
                    }
                    "setTxVgaGain" -> {
                        val gain = call.argument<Int>("gain") ?: 0
                        val success = hackrf?.setTxVGAGain(gain)
                        Log.d(TAG, "setTxVgaGain to $gain, Success: $success")
                        result.success(success)
                    }
                    "startTx" -> {
                        txQueue = hackrf?.startTX()
                        isTransmitting = (txQueue != null)
                        Log.d(TAG, "startTx, Success: $isTransmitting")
                        result.success(isTransmitting)
                    }
                    "stopTx" -> {
                        hackrf?.stop()
                        isTransmitting = false
                        txQueue = null
                        Log.d(TAG, "stopTx called.")
                        result.success(true)
                    }
                    "sendData" -> {
                        val data = call.argument<ByteArray>("data")
                        if (isTransmitting && data != null) {
                            Log.d(TAG, "Sending data to queue, size: ${data.size} bytes...")
                            txQueue?.put(data) // This is a blocking call
                            Log.d(TAG, "Data sent successfully.")
                            result.success(null)
                        } else {
                            Log.w(TAG, "sendData failed. Transmitting: $isTransmitting, Data is null: ${data == null}")
                            result.error("TX_ERROR", "Not transmitting or data is null.", null)
                        }
                    }
                    else -> result.notImplemented()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ EXCEPTION in onMethodCall: ${e.message}", e)
                result.error("NATIVE_ERROR", e.message, e.stackTraceToString())
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        hackrf?.stop()
        Log.d(TAG, "Plugin detached from engine.")
        hackrf = null
        pluginBinding = null
        pendingInitResult = null
    }
}