import 'package:flutter/material.dart';
import '../../services/platform_service.dart';

class WelcomeScreen extends StatefulWidget {
  const WelcomeScreen({super.key});

  @override
  State<WelcomeScreen> createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> {
  bool _hasRegisteredFaces = false;
  bool _hasPinSet = false;

  @override
  void initState() {
    super.initState();
    _checkExistingSetup();
  }

  Future<void> _checkExistingSetup() async {
    final faces = await PlatformService.getRegisteredFaces();
    final pinSet = await PlatformService.isPinSet();
    if (mounted) {
      setState(() {
        _hasRegisteredFaces = faces.isNotEmpty;
        _hasPinSet = pinSet;
      });
    }
  }

  void _navigateToNextStep() {
    if (!_hasRegisteredFaces) {
      Navigator.pushNamed(context, '/onboarding/face-registration');
    } else if (!_hasPinSet) {
      Navigator.pushNamed(context, '/onboarding/pin-setup');
    } else {
      Navigator.pushNamed(context, '/onboarding/permissions');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 48),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Spacer(),
              // App icon
              Container(
                width: 120,
                height: 120,
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(30),
                ),
                child: Icon(
                  Icons.shield,
                  size: 64,
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
              const SizedBox(height: 32),
              Text(
                'KidShield',
                style: Theme.of(context).textTheme.headlineLarge?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
              ),
              const SizedBox(height: 12),
              Text(
                'Protect your child\'s screen time\nwith face recognition',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                      color: Colors.grey[600],
                    ),
              ),
              const Spacer(),
              // How it works section
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  children: [
                    Text(
                      'How it works',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                    const SizedBox(height: 12),
                    _buildStep(context, '1', 'Register your face as the parent'),
                    const SizedBox(height: 8),
                    _buildStep(context, '2', 'Set a fallback PIN'),
                    const SizedBox(height: 8),
                    _buildStep(context, '3', 'Grant required permissions'),
                    const SizedBox(height: 8),
                    _buildStep(context, '4', 'Select apps to restrict'),
                  ],
                ),
              ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                height: 56,
                child: FilledButton(
                  onPressed: _navigateToNextStep,
                  child: Text(
                    _hasRegisteredFaces ? 'Continue Setup' : 'Get Started',
                    style: const TextStyle(fontSize: 18),
                  ),
                ),
              ),
              if (_hasRegisteredFaces)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    'Face already registered — resuming setup',
                    style: TextStyle(color: Colors.grey[500], fontSize: 13),
                  ),
                ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  static Widget _buildStep(BuildContext context, String number, String text) {
    return Row(
      children: [
        Container(
          width: 28,
          height: 28,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.primary,
            shape: BoxShape.circle,
          ),
          child: Center(
            child: Text(
              number,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 14,
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(text, style: Theme.of(context).textTheme.bodyMedium),
        ),
      ],
    );
  }
}
