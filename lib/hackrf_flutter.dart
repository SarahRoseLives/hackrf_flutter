import 'dart:typed_data';

import 'package:flutter/services.dart';

class HackrfFlutter {
  // The channel name must match the one in your native code
  static const MethodChannel _channel = MethodChannel('hackrf_flutter_channel');

  /// Initializes the HackRF device by finding it and requesting USB permission.
  /// Returns `true` on success, `false` on failure.
  Future<bool> init() async {
    final bool? isInitialized = await _channel.invokeMethod('init');
    return isInitialized ?? false;
  }

  /// Gets the Board ID from the HackRF device.
  Future<int?> getBoardId() async {
    final int? boardId = await _channel.invokeMethod('getBoardId');
    return boardId;
  }

  /// Sets the center frequency of the HackRF.
  /// [freqHz] : Frequency in Hz.
  Future<bool> setFrequency(int freqHz) async {
    final bool? result = await _channel.invokeMethod('setFrequency', {'freq': freqHz});
    return result ?? false;
  }

  /// Sets the sample rate of the HackRF.
  /// [rateHz] : Sample rate in Hz.
  Future<bool> setSampleRate(int rateHz) async {
    final bool? result = await _channel.invokeMethod('setSampleRate', {'rate': rateHz});
    return result ?? false;
  }

  /// Sets the transmit VGA (IF) gain.
  /// [gain] : Gain value, 0-47.
  Future<bool> setTxVgaGain(int gain) async {
    final bool? result = await _channel.invokeMethod('setTxVgaGain', {'gain': gain});
    return result ?? false;
  }

  /// Starts the HackRF's transmitter.
  Future<bool> startTx() async {
    final bool? result = await _channel.invokeMethod('startTx');
    return result ?? false;
  }

  /// Stops the HackRF's transmitter.
  Future<bool> stopTx() async {
    final bool? result = await _channel.invokeMethod('stopTx');
    return result ?? false;
  }

  /// Sends a block of I/Q samples to the transmitter queue.
  /// The data should be interleaved 8-bit signed integers (I, Q, I, Q, ...).
  Future<void> sendData(Uint8List data) async {
    await _channel.invokeMethod('sendData', {'data': data});
  }
}