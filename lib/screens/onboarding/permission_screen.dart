import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import '../../services/platform_service.dart';

class PermissionScreen extends StatefulWidget {
  const PermissionScreen({super.key});

  @override
  State<PermissionScreen> createState() => _PermissionScreenState();
}

class _PermissionScreenState extends State<PermissionScreen>
    with WidgetsBindingObserver {
  final Map<String, _PermissionItem> _permissions = {};
  bool _allGranted = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _initPermissions();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkAllPermissions();
    }
  }

  void _initPermissions() {
    _permissions['camera'] = _PermissionItem(
      title: 'Camera',
      description: 'Required for face recognition verification',
      icon: Icons.camera_alt,
      isGranted: false,
    );
    _permissions['usageStats'] = _PermissionItem(
      title: 'Usage Access',
      description: 'Required — detects which app is in the foreground to block restricted apps',
      icon: Icons.bar_chart,
      isGranted: false,
    );
    _permissions['overlay'] = _PermissionItem(
      title: 'Display Over Other Apps',
      description: 'Shows the blocking screen over restricted apps',
      icon: Icons.layers,
      isGranted: false,
    );
    _permissions['deviceAdmin'] = _PermissionItem(
      title: 'Device Administrator',
      description: 'Prevents children from uninstalling the app',
      icon: Icons.admin_panel_settings,
      isGranted: false,
    );
    _permissions['battery'] = _PermissionItem(
      title: 'Battery Optimization',
      description: 'Keeps the protection service running reliably',
      icon: Icons.battery_charging_full,
      isGranted: false,
    );
    _permissions['notification'] = _PermissionItem(
      title: 'Notifications',
      description: 'Shows a persistent notification while protection is active',
      icon: Icons.notifications,
      isGranted: false,
    );
    _permissions['accessibility'] = _PermissionItem(
      title: 'Accessibility (Optional)',
      description: 'Speeds up app detection from ~300ms to instant. '
          'Not required — skip if "Restricted setting" appears.',
      icon: Icons.accessibility,
      isGranted: false,
      isOptional: true,
    );

    _checkAllPermissions();
  }

  Future<void> _checkAllPermissions() async {
    final cameraStatus = await Permission.camera.status;
    final notifStatus = await Permission.notification.status;
    final usageStatsGranted =
        await PlatformService.checkUsageStatsPermission();
    final accessibilityGranted =
        await PlatformService.checkAccessibilityPermission();
    final overlayGranted = await PlatformService.checkOverlayPermission();
    final deviceAdminGranted =
        await PlatformService.checkDeviceAdminPermission();
    final batteryGranted = await PlatformService.checkBatteryOptimization();

    setState(() {
      _permissions['camera']!.isGranted = cameraStatus.isGranted;
      _permissions['notification']!.isGranted = notifStatus.isGranted;
      _permissions['usageStats']!.isGranted = usageStatsGranted;
      _permissions['accessibility']!.isGranted = accessibilityGranted;
      _permissions['overlay']!.isGranted = overlayGranted;
      _permissions['deviceAdmin']!.isGranted = deviceAdminGranted;
      _permissions['battery']!.isGranted = batteryGranted;

      // All required permissions (optional ones excluded from gate check)
      _allGranted = _permissions.entries
          .where((e) => !e.value.isOptional)
          .every((e) => e.value.isGranted);
    });
  }

  Future<void> _requestPermission(String key) async {
    switch (key) {
      case 'camera':
        await Permission.camera.request();
        break;
      case 'notification':
        await Permission.notification.request();
        break;
      case 'usageStats':
        await PlatformService.requestUsageStatsPermission();
        break;
      case 'accessibility':
        await PlatformService.requestAccessibilityPermission();
        break;
      case 'overlay':
        await PlatformService.requestOverlayPermission();
        break;
      case 'deviceAdmin':
        await PlatformService.requestDeviceAdminPermission();
        break;
      case 'battery':
        await PlatformService.requestBatteryOptimization();
        break;
    }

    // Give time for the user to return
    await Future.delayed(const Duration(milliseconds: 500));
    _checkAllPermissions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Permissions')),
      body: Column(
        children: [
          // Header
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            color: _allGranted
                ? Colors.green.withValues(alpha: 0.1)
                : Colors.orange.withValues(alpha: 0.1),
            child: Row(
              children: [
                Icon(
                  _allGranted ? Icons.check_circle : Icons.warning,
                  color: _allGranted ? Colors.green : Colors.orange,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    _allGranted
                        ? 'All required permissions granted!'
                        : 'Please grant the required permissions for KidShield to work.',
                    style: TextStyle(
                      color: _allGranted ? Colors.green[800] : Colors.orange[800],
                    ),
                  ),
                ),
              ],
            ),
          ),

          // Permission list
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: _permissions.entries.map((entry) {
                final key = entry.key;
                final item = entry.value;
                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  color: item.isOptional
                      ? Colors.grey.withValues(alpha: 0.04)
                      : null,
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: item.isGranted
                          ? Colors.green.withValues(alpha: 0.2)
                          : item.isOptional
                              ? Colors.blue.withValues(alpha: 0.1)
                              : Colors.grey.withValues(alpha: 0.2),
                      child: Icon(
                        item.icon,
                        color: item.isGranted
                            ? Colors.green
                            : item.isOptional
                                ? Colors.blue
                                : Colors.grey,
                      ),
                    ),
                    title: Text(
                      item.title,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    subtitle: Text(
                      item.description,
                      style: const TextStyle(fontSize: 12),
                    ),
                    trailing: item.isGranted
                        ? const Icon(Icons.check_circle, color: Colors.green)
                        : TextButton(
                            onPressed: () => _requestPermission(key),
                            child: Text(item.isOptional ? 'Enable' : 'Grant'),
                          ),
                  ),
                );
              }).toList(),
            ),
          ),

          // Continue / Skip buttons
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                SizedBox(
                  width: double.infinity,
                  height: 56,
                  child: FilledButton(
                    onPressed: _allGranted
                        ? () {
                            Navigator.pushNamed(context, '/onboarding/app-selection');
                          }
                        : null,
                    child: const Text(
                      'Continue',
                      style: TextStyle(fontSize: 16),
                    ),
                  ),
                ),
                if (!_allGranted)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: TextButton(
                      onPressed: () {
                        showDialog(
                          context: context,
                          builder: (ctx) => AlertDialog(
                            title: const Text('Skip Permissions?'),
                            content: const Text(
                              'KidShield may not work correctly without all permissions. '
                              'You can grant them later from Settings.',
                            ),
                            actions: [
                              TextButton(
                                onPressed: () => Navigator.pop(ctx),
                                child: const Text('Go Back'),
                              ),
                              FilledButton(
                                onPressed: () {
                                  Navigator.pop(ctx);
                                  Navigator.pushNamed(context, '/onboarding/app-selection');
                                },
                                child: const Text('Skip Anyway'),
                              ),
                            ],
                          ),
                        );
                      },
                      child: const Text('Skip for now'),
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

class _PermissionItem {
  final String title;
  final String description;
  final IconData icon;
  final bool isOptional;
  bool isGranted;

  _PermissionItem({
    required this.title,
    required this.description,
    required this.icon,
    required this.isGranted,
    this.isOptional = false,
  });
}
