import 'package:flutter/material.dart';
import 'dart:async';
import 'package:hackrf_flutter/hackrf_flutter.dart'; // Import your plugin

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _status = 'Not Initialized';
  final _hackrfFlutterPlugin = HackrfFlutter();

  @override
  void initState() {
    super.initState();
    initHackrfDevice();
  }

  Future<void> initHackrfDevice() async {
    bool? isInitialized = await _hackrfFlutterPlugin.init();
    setState(() {
      _status = isInitialized ?? false ? 'HackRF Initialized!' : 'Failed to initialize.';
    });

    if (isInitialized ?? false) {
      int? boardId = await _hackrfFlutterPlugin.getBoardId();
      setState(() {
        _status = 'HackRF Initialized! Board ID: $boardId';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('HackRF Plugin Example'),
        ),
        body: Center(
          child: Text('Status: $_status\n'),
        ),
      ),
    );
  }
}
