import 'package:flutter/services.dart';

class HackrfFlutter {
  // The channel name must match the one in your Java/Kotlin code
  static const MethodChannel _channel = MethodChannel('hackrf_flutter_channel');

  /// Initializes the HackRF device.
  /// Returns `true` on success, `false` on failure.
  Future<bool?> init() async {
    try {
      final bool? isInitialized = await _channel.invokeMethod('init');
      return isInitialized;
    } on PlatformException catch (e) {
      // Handle potential errors from the native side
      print("Failed to initialize Hackrf: '${e.message}'.");
      return false;
    }
  }

  /// Gets the Board ID from the HackRF device.
  /// Throws an exception if not initialized or if there's an error.
  Future<int?> getBoardId() async {
    try {
      final int? boardId = await _channel.invokeMethod('getBoardId');
      return boardId;
    } on PlatformException catch (e) {
      print("Failed to get Board ID: '${e.message}'.");
      return null;
    }
  }
}