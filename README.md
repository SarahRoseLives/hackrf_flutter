# HackRF Flutter Plugin

A Flutter plugin for interfacing with HackRF One software-defined radios on Android. This library bridges your Flutter app and the native [`hackrf_android`](https://github.com/demantz/hackrf_android) Java library, enabling direct control and transmission via HackRF from Dart.

Ideal for mobile SDR (Software Defined Radio) applications, remote-control transmitters, educational radio tools, and more.

---

# HackRF Flutter Plugin: Function Guide

This guide provides a practical, end-user overview of all available functions in the `hackrf_flutter` plugin and how to use them in your Flutter app.  
**All functions are asynchronous (`Future`) for easy integration with modern Flutter code.**

---

## 1. Initialization & Device Info

You **must initialize** the device before calling any other function.

### `init()`

Finds the connected HackRF, requests USB permissions, and prepares the device for use.

- **Returns:** `Future<bool>` (true if successful, false otherwise)

```dart
import 'package:hackrf_flutter/hackrf_flutter.dart';

final _hackrf = HackrfFlutter();

Future<void> connectToDevice() async {
  bool isReady = await _hackrf.init();
  if (isReady) {
    print("HackRF is connected and ready.");
  } else {
    print("Could not find or connect to HackRF.");
  }
}
```

### `getBoardId()`

Retrieves the board ID of the connected HackRF.

- **Returns:** `Future<int?>` (board ID, or null if not found)

```dart
int? boardId = await _hackrf.getBoardId();
print("Device Board ID: $boardId"); // e.g., 2 for HackRF One
```

---

## 2. Common Radio Configuration

Configure parameters used by both transmitter and receiver.

### `setFrequency(int freqHz)`

Sets the center frequency of the HackRF.

- **freqHz:** Frequency in Hertz (e.g., `145100000` for 145.1 MHz)
- **Returns:** `Future<bool>` (true on success)

```dart
await _hackrf.setFrequency(145100000);
```

### `setSampleRate(int rateHz)`

Sets the sample rate.

- **rateHz:** Sample rate in Hertz (e.g., `8000000` for 8 MSPS)
- **Returns:** `Future<bool>` (true on success)

```dart
await _hackrf.setSampleRate(8000000);
```

---

## 3. Transmission (TX) Functions

Functions for transmitting signals.

### `setTxVgaGain(int gain)`

Sets the transmit VGA (baseband) gain.

- **gain:** Integer from `0` to `47`
- **Returns:** `Future<bool>` (true on success)

```dart
await _hackrf.setTxVgaGain(30); // Set gain to 30
```

### `startTx()`

Puts the HackRF into transmit mode.  
After calling this, you can begin sending data.

- **Returns:** `Future<bool>` (true on success)

### `stopTx()`

Takes the HackRF out of transmit mode.

- **Returns:** `Future<bool>` (true on success)

### `sendData(Uint8List data)`

Sends a packet of I/Q data to the HackRF's transmit queue.

- **data:** A `Uint8List` containing interleaved 8-bit I/Q samples.
- **NOTE:** The native library expects a specific packet size of **262,144 bytes**.

```dart
// Create a buffer of the required size
const packetSize = 262144;
final iqData = Uint8List(packetSize);

// ... fill iqData with your signal ...

await _hackrf.startTx();
await _hackrf.sendData(iqData); // Send the packet
await _hackrf.stopTx();
```

---

## 4. Receiving (RX) Functions

Functions for receiving signals.

### `setRxLnaGain(int gain)`

Sets the receive LNA (low-noise amplifier / RF) gain.

- **gain:** Integer from `0` to `40`, in steps of 8 (i.e., `0`, `8`, `16`, `24`, `32`, `40`)
- **Returns:** `Future<bool>` (true on success)

```dart
await _hackrf.setRxLnaGain(32);
```

### `setRxVgaGain(int gain)`

Sets the receive VGA (baseband) gain.

- **gain:** Integer from `0` to `62`, in steps of 2
- **Returns:** `Future<bool>` (true on success)

```dart
await _hackrf.setRxVgaGain(20);
```

### `startRx()`

Begins listening for incoming I/Q data from the HackRF.  
The hardware starts when you subscribe to the returned stream.

- **Returns:** `Stream<Uint8List>` (emits packets of I/Q data)

### `stopRx()`

Stops the receiver.  
Usually handled automatically when you cancel the stream subscription, but you can call for safety.

- **Returns:** `Future<bool>` (true on success)

---

### Example: Listening to RX Data

```dart
StreamSubscription<Uint8List>? rxSubscription;

Future<void> startListening() async {
  // 1. Configure the radio for receiving
  await _hackrf.setFrequency(101100000); // 101.1 MHz FM
  await _hackrf.setSampleRate(2000000);
  await _hackrf.setRxLnaGain(24);
  await _hackrf.setRxVgaGain(20);

  // 2. Get the stream and subscribe to it
  final rxStream = _hackrf.startRx();
  rxSubscription = rxStream.listen(
    (data) {
      print("Received a packet with ${data.length} bytes.");
      // ... process your I/Q data here ...
    },
    onError: (e) {
      print("An error occurred in the RX stream: $e");
    },
    onDone: () {
      print("The RX stream is finished.");
    }
  );
}

void stopListening() {
  // 3. To stop receiving, just cancel the subscription
  rxSubscription?.cancel();
  print("Stopped listening.");
}
```

---

## Quick Reference

| Function               | Purpose                                     | Returns         |
|------------------------|---------------------------------------------|-----------------|
| `init()`               | Initialize device, request USB              | `Future<bool>`  |
| `getBoardId()`         | Get HackRF board ID                         | `Future<int?>`  |
| `setFrequency()`       | Set center frequency (Hz)                   | `Future<bool>`  |
| `setSampleRate()`      | Set sample rate (Hz)                        | `Future<bool>`  |
| `setTxVgaGain()`       | Set transmit VGA gain                       | `Future<bool>`  |
| `startTx()`            | Start transmission                          | `Future<bool>`  |
| `stopTx()`             | Stop transmission                           | `Future<bool>`  |
| `sendData()`           | Send I/Q data (Uint8List, 262,144 bytes)    | `Future<bool>`  |
| `setRxLnaGain()`       | Set RX LNA gain                             | `Future<bool>`  |
| `setRxVgaGain()`       | Set RX VGA gain                             | `Future<bool>`  |
| `startRx()`            | Start RX, returns stream of I/Q data        | `Stream<Uint8List>` |
| `stopRx()`             | Stop RX                                     | `Future<bool>`  |

---

**Tip:** Always check the return value of any function for error handling in your app.

---

## How It Works

- **Flutter (Dart):** You call methods such as `_hackrf.setFrequency()`.
- **MethodChannel:** Dart sends method calls to the native side using Flutter MethodChannel.
- **Native Plugin (`HackrfFlutterPlugin.kt`):** Receives calls, delegates to Java code.
- **Java Library (`Hackrf.java` in [demantz/hackrf_android](https://github.com/demantz/hackrf_android)):** Handles USB communication with HackRF.
- **HackRF Device:** Executes the requested command.

This plugin is a direct bridge to [hackrf_android](https://github.com/demantz/hackrf_android)—all core SDR logic and USB handling is provided by that Java library.
