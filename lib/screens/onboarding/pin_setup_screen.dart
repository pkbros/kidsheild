import 'package:flutter/material.dart';
import '../../services/platform_service.dart';

class PinSetupScreen extends StatefulWidget {
  const PinSetupScreen({super.key});

  @override
  State<PinSetupScreen> createState() => _PinSetupScreenState();
}

class _PinSetupScreenState extends State<PinSetupScreen> {
  final _pinController = TextEditingController();
  final _confirmController = TextEditingController();
  String? _errorMessage;
  bool _isSettingPin = false;

  Future<void> _setPin() async {
    final pin = _pinController.text;
    final confirm = _confirmController.text;

    if (pin.length < 6) {
      setState(() => _errorMessage = 'PIN must be at least 6 digits');
      return;
    }

    if (pin != confirm) {
      setState(() => _errorMessage = 'PINs do not match');
      return;
    }

    setState(() {
      _errorMessage = null;
      _isSettingPin = true;
    });

    try {
      await PlatformService.setPin(pin);
      if (mounted) {
        Navigator.pushNamed(context, '/onboarding/permissions');
      }
    } catch (e) {
      setState(() => _errorMessage = 'Failed to set PIN: $e');
    } finally {
      setState(() => _isSettingPin = false);
    }
  }

  @override
  void dispose() {
    _pinController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Set Fallback PIN')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(32),
          child: Column(
            children: [
              const SizedBox(height: 32),
              Icon(Icons.pin, size: 64, color: Colors.grey[400]),
              const SizedBox(height: 24),
            Text(
              'Set a Fallback PIN',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              'Use this PIN when face recognition is unavailable\n(e.g., poor lighting, camera issues)',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 32),
            TextField(
              controller: _pinController,
              keyboardType: TextInputType.number,
              obscureText: true,
              maxLength: 10,
              decoration: const InputDecoration(
                labelText: 'PIN (6+ digits)',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.lock),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _confirmController,
              keyboardType: TextInputType.number,
              obscureText: true,
              maxLength: 10,
              decoration: const InputDecoration(
                labelText: 'Confirm PIN',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.lock_outline),
              ),
            ),
            if (_errorMessage != null) ...[
              const SizedBox(height: 12),
              Text(
                _errorMessage!,
                style: const TextStyle(color: Colors.red),
              ),
            ],
            const SizedBox(height: 32),
            SizedBox(
              width: double.infinity,
              height: 56,
              child: FilledButton(
                onPressed: _isSettingPin ? null : _setPin,
                child: _isSettingPin
                    ? const CircularProgressIndicator(color: Colors.white)
                    : const Text('Set PIN & Continue',
                        style: TextStyle(fontSize: 16)),
              ),
            ),
            const SizedBox(height: 32),
          ],
          ),
        ),
      ),
    );
  }
}
