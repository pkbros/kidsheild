import 'dart:typed_data';
import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import '../../services/platform_service.dart';

class FaceRegistrationScreen extends StatefulWidget {
  const FaceRegistrationScreen({super.key});

  @override
  State<FaceRegistrationScreen> createState() => _FaceRegistrationScreenState();
}

class _FaceRegistrationScreenState extends State<FaceRegistrationScreen> {
  CameraController? _cameraController;
  bool _isInitialized = false;
  bool _isProcessing = false;
  int _captureCount = 0;
  final int _requiredCaptures = 3;
  String _statusMessage = 'Position your face in the circle';
  final List<String> _captureInstructions = [
    'Look straight at the camera',
    'Turn your head slightly left',
    'Turn your head slightly right',
  ];
  String _parentName = 'Parent';
  final _nameController = TextEditingController(text: 'Parent');
  bool _nameEntered = false;

  @override
  void initState() {
    super.initState();
    _initCamera();
    PlatformService.initFaceEngine();
  }

  Future<void> _initCamera() async {
    final cameras = await availableCameras();
    final frontCamera = cameras.firstWhere(
      (c) => c.lensDirection == CameraLensDirection.front,
      orElse: () => cameras.first,
    );

    _cameraController = CameraController(
      frontCamera,
      ResolutionPreset.medium,
      enableAudio: false,
    );

    await _cameraController!.initialize();
    if (mounted) {
      setState(() => _isInitialized = true);
    }
  }

  Future<void> _captureAndProcess() async {
    if (_isProcessing || _cameraController == null) return;
    setState(() {
      _isProcessing = true;
      _statusMessage = 'Processing...';
    });

    try {
      final image = await _cameraController!.takePicture();
      final bytes = await image.readAsBytes();

      final result = await PlatformService.processAndRegisterFace(
        Uint8List.fromList(bytes),
        _parentName,
      );

      if (result['success'] == true) {
        setState(() {
          _captureCount++;
          if (_captureCount < _requiredCaptures) {
            _statusMessage = _captureInstructions[_captureCount];
          } else {
            _statusMessage = 'Face registration complete!';
          }
        });

        if (_captureCount >= _requiredCaptures) {
          await Future.delayed(const Duration(seconds: 1));
          if (mounted) {
            Navigator.pushNamed(context, '/onboarding/add-guardians');
          }
        }
      } else {
        setState(() {
          _statusMessage = result['error'] ?? 'No face detected. Try again.';
        });
      }
    } catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.toString()}';
      });
    } finally {
      setState(() => _isProcessing = false);
    }
  }

  @override
  void dispose() {
    _cameraController?.dispose();
    _nameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (!_nameEntered) {
      return _buildNameEntry();
    }
    return _buildCameraCapture();
  }

  Widget _buildNameEntry() {
    return Scaffold(
      appBar: AppBar(title: const Text('Register Parent')),
      body: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.person, size: 80, color: Colors.grey[400]),
            const SizedBox(height: 24),
            Text(
              'What should we call you?',
              style: Theme.of(context).textTheme.headlineSmall,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              'This name will be associated with your face profile.',
              style: TextStyle(color: Colors.grey[600]),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            TextField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Your Name',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.badge),
              ),
              textCapitalization: TextCapitalization.words,
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: FilledButton(
                onPressed: () {
                  if (_nameController.text.trim().isNotEmpty) {
                    setState(() {
                      _parentName = _nameController.text.trim();
                      _nameEntered = true;
                      _statusMessage = _captureInstructions[0];
                    });
                  }
                },
                child: const Text('Continue'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCameraCapture() {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Face Registration'),
        actions: [
          if (_captureCount == 0)
            TextButton(
              onPressed: () async {
                final faces = await PlatformService.getRegisteredFaces();
                if (!mounted) return;
                if (faces.isNotEmpty) {
                  Navigator.pushNamed(context, '/onboarding/add-guardians');
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Register at least one face first'),
                    ),
                  );
                }
              },
              child: const Text('Skip'),
            ),
          Center(
            child: Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Text(
                '$_captureCount/$_requiredCaptures',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          // Progress bar
          LinearProgressIndicator(
            value: _captureCount / _requiredCaptures,
            minHeight: 4,
          ),
          Expanded(
            child: _isInitialized
                ? Stack(
                    alignment: Alignment.center,
                    children: [
                      // Camera preview
                      ClipRRect(
                        borderRadius: BorderRadius.circular(16),
                        child: AspectRatio(
                          aspectRatio: 3 / 4,
                          child: CameraPreview(_cameraController!),
                        ),
                      ),
                      // Face guide overlay
                      Container(
                        width: 240,
                        height: 300,
                        decoration: BoxDecoration(
                          border: Border.all(
                            color: _isProcessing
                                ? Colors.orange
                                : Colors.white.withValues(alpha: 0.8),
                            width: 3,
                          ),
                          borderRadius: BorderRadius.circular(120),
                        ),
                      ),
                    ],
                  )
                : const Center(child: CircularProgressIndicator()),
          ),
          // Status and capture button
          Container(
            padding: const EdgeInsets.all(24),
            child: Column(
              children: [
                Text(
                  _statusMessage,
                  style: Theme.of(context).textTheme.titleMedium,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 20),
                if (_captureCount < _requiredCaptures)
                  SizedBox(
                    width: 80,
                    height: 80,
                    child: FloatingActionButton(
                      onPressed: _isProcessing ? null : _captureAndProcess,
                      shape: const CircleBorder(),
                      child: _isProcessing
                          ? const CircularProgressIndicator(color: Colors.white)
                          : const Icon(Icons.camera, size: 36),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
