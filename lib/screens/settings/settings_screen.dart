import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/app_state.dart';
import '../../services/platform_service.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, _) {
        return Scaffold(
          appBar: AppBar(title: const Text('Settings')),
          body: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // Protection toggle
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
                ),
                child: SwitchListTile(
                  title: const Text(
                    'Protection Enabled',
                    style: TextStyle(fontWeight: FontWeight.w600),
                  ),
                  subtitle: Text(
                    appState.isProtectionEnabled
                        ? 'Blocking restricted apps'
                        : 'Protection is paused',
                  ),
                  value: appState.isProtectionEnabled,
                  onChanged: (value) async {
                    await appState.setProtectionEnabled(value);
                  },
                  secondary: Icon(
                    appState.isProtectionEnabled
                        ? Icons.shield
                        : Icons.shield_outlined,
                    color: appState.isProtectionEnabled
                        ? Colors.green
                        : Colors.grey,
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // Section: App Management
              _buildSectionHeader(context, 'App Management'),
              _buildSettingsTile(
                context,
                icon: Icons.apps,
                title: 'Manage Blocked Apps',
                subtitle: '${appState.blockedApps.length} apps blocked',
                onTap: () => Navigator.pushNamed(context, '/manage-apps'),
              ),

              const SizedBox(height: 16),

              // Section: Security
              _buildSectionHeader(context, 'Security'),
              _buildSettingsTile(
                context,
                icon: Icons.face,
                title: 'Manage Faces',
                subtitle: '${appState.registeredFaces.length} faces registered',
                onTap: () {
                  Navigator.pushNamed(context, '/manage-faces');
                },
              ),
              _buildSettingsTile(
                context,
                icon: Icons.pin,
                title: 'Change PIN',
                subtitle: 'Update your fallback PIN',
                onTap: () => _showChangePinDialog(context),
              ),

              const SizedBox(height: 16),

              // Section: Timing
              _buildSectionHeader(context, 'Timing'),
              _buildSettingsTile(
                context,
                icon: Icons.timer,
                title: 'Re-verification Interval',
                subtitle: appState.reverificationInterval == 0
                    ? 'Every time an app is opened'
                    : _formatInterval(appState.reverificationInterval),
                onTap: () => _showIntervalPicker(context, appState),
              ),

              const SizedBox(height: 16),

              // Section: Buddy Mascot
              _buildSectionHeader(context, 'Overlay Style'),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
                ),
                child: Column(
                  children: [
                    RadioListTile<String>(
                      title: const Text('Video Buddy'),
                      subtitle: const Text('Full-screen video with fun messages'),
                      value: 'video',
                      groupValue: appState.overlayMode,
                      onChanged: (value) async {
                        if (value != null) await appState.setOverlayMode(value);
                      },
                      secondary: Icon(
                        Icons.play_circle_fill,
                        color: appState.overlayMode == 'video'
                            ? Colors.deepPurple
                            : Colors.grey,
                      ),
                    ),
                    RadioListTile<String>(
                      title: const Text('Static Buddy'),
                      subtitle: const Text('Illustrated mascot with speech bubbles'),
                      value: 'buddy',
                      groupValue: appState.overlayMode,
                      onChanged: (value) async {
                        if (value != null) await appState.setOverlayMode(value);
                      },
                      secondary: Icon(
                        Icons.emoji_people,
                        color: appState.overlayMode == 'buddy'
                            ? Colors.orange
                            : Colors.grey,
                      ),
                    ),
                    RadioListTile<String>(
                      title: const Text('Classic'),
                      subtitle: const Text('Simple blocking overlay'),
                      value: 'classic',
                      groupValue: appState.overlayMode,
                      onChanged: (value) async {
                        if (value != null) await appState.setOverlayMode(value);
                      },
                      secondary: Icon(
                        Icons.block,
                        color: appState.overlayMode == 'classic'
                            ? Colors.blueGrey
                            : Colors.grey,
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 16),

              // Section: Parent Self-Awareness
              _buildSectionHeader(context, 'Parent Self-Awareness'),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
                ),
                child: SwitchListTile(
                  title: const Text('Track My Screen Time'),
                  subtitle: const Text(
                    'Show your own screen time on the dashboard alongside your child\'s',
                  ),
                  value: appState.isParentTrackingEnabled,
                  onChanged: (v) => appState.setParentTrackingEnabled(v),
                  secondary: Icon(
                    Icons.person,
                    color: appState.isParentTrackingEnabled
                        ? Colors.purple
                        : Colors.grey,
                  ),
                ),
              ),
              const SizedBox(height: 8),

              // Nudge Threshold
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
                ),
                child: ListTile(
                  leading: const Icon(Icons.notifications_active,
                      color: Colors.orange),
                  title: const Text('Nudge Threshold'),
                  subtitle: Text(
                    appState.nudgeThreshold <= 0
                        ? 'Disabled'
                        : '${appState.nudgeThreshold} attempts / hour',
                  ),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _showNudgeThresholdPicker(context, appState),
                ),
              ),

              const SizedBox(height: 16),

              // Section: Detection Engine
              _buildSectionHeader(context, 'Detection Engine'),
              _buildDetectionModeCard(context, appState),

              const SizedBox(height: 16),

              // Section: System
              _buildSectionHeader(context, 'System'),
              _buildSettingsTile(
                context,
                icon: Icons.health_and_safety,
                title: 'Permission Health Check',
                subtitle: 'Verify all permissions are active',
                onTap: () => Navigator.pushNamed(context, '/permission-check'),
              ),

              if (appState.isDebugMode) ...[
                const SizedBox(height: 16),
                _buildSectionHeader(context, 'Debug'),
                _buildSettingsTile(
                  context,
                  icon: Icons.bug_report,
                  title: 'Clear Verification Sessions',
                  subtitle: 'Reset all app unlock timestamps',
                  onTap: () async {
                    await PlatformService.clearVerificationSessions();
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Sessions cleared')),
                      );
                    }
                  },
                ),
                Card(
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                    side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
                  ),
                  child: SwitchListTile(
                    title: const Text('Debug Mode'),
                    subtitle: const Text('Show debug controls on dashboard'),
                    value: appState.isDebugMode,
                    onChanged: (v) => appState.setDebugMode(v),
                    secondary: const Icon(Icons.developer_mode),
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _buildSectionHeader(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8),
      child: Text(
        title,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.bold,
              color: Theme.of(context).colorScheme.primary,
            ),
      ),
    );
  }

  Widget _buildDetectionModeCard(BuildContext context, AppState appState) {
    final mode = appState.detectionMode;
    final modeStr = mode['mode'] as String? ?? 'none';
    final usageGranted = mode['usageStatsGranted'] as bool? ?? false;
    final a11yEnabled = mode['accessibilityEnabled'] as bool? ?? false;

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  modeStr == 'hybrid'
                      ? Icons.bolt
                      : modeStr == 'none'
                          ? Icons.warning
                          : Icons.poll,
                  color: modeStr == 'none' ? Colors.red : Colors.green,
                ),
                const SizedBox(width: 8),
                Text(
                  appState.detectionModeLabel,
                  style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // UsageStats status
            _buildDetectionRow(
              'Usage Stats (Primary)',
              usageGranted,
              usageGranted ? null : () async {
                await PlatformService.requestUsageStatsPermission();
              },
            ),
            const SizedBox(height: 8),

            // Accessibility status
            _buildDetectionRow(
              'Accessibility (Optional Boost)',
              a11yEnabled,
              a11yEnabled ? null : () async {
                await PlatformService.requestAccessibilityPermission();
              },
            ),

            if (!usageGranted) ...[
              const SizedBox(height: 12),
              Text(
                'Usage Access is required for app detection to work.',
                style: TextStyle(fontSize: 12, color: Colors.red[700]),
              ),
            ] else if (!a11yEnabled) ...[
              const SizedBox(height: 12),
              Text(
                'Enable Accessibility for instant (0ms) detection. '
                'Without it, polling detects apps in ~300ms.',
                style: TextStyle(fontSize: 12, color: Colors.grey[600]),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildDetectionRow(String label, bool enabled, VoidCallback? onEnable) {
    return Row(
      children: [
        Icon(
          enabled ? Icons.check_circle : Icons.radio_button_unchecked,
          size: 18,
          color: enabled ? Colors.green : Colors.grey,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(label, style: const TextStyle(fontSize: 13)),
        ),
        if (onEnable != null)
          TextButton(
            onPressed: onEnable,
            style: TextButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              minimumSize: const Size(0, 32),
            ),
            child: const Text('Enable', style: TextStyle(fontSize: 12)),
          )
        else
          Text('Active', style: TextStyle(fontSize: 12, color: Colors.green[600])),
      ],
    );
  }

  Widget _buildSettingsTile(
    BuildContext context, {
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return Card(
      elevation: 0,
      margin: const EdgeInsets.only(bottom: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
      ),
      child: ListTile(
        leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w500)),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 12)),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }

  static String _formatInterval(int seconds) {
    if (seconds <= 0) return 'Every time';
    if (seconds < 60) return '${seconds}s';
    if (seconds < 3600) {
      final m = seconds ~/ 60;
      final s = seconds % 60;
      return s == 0 ? '${m}m' : '${m}m ${s}s';
    }
    final h = seconds ~/ 3600;
    final m = (seconds % 3600) ~/ 60;
    return m == 0 ? '${h}h' : '${h}h ${m}m';
  }

  Future<void> _showIntervalPicker(
    BuildContext context,
    AppState appState,
  ) async {
    final controller = TextEditingController(
      text: appState.reverificationInterval.toString(),
    );

    // Quick-pick presets (in seconds)
    final presets = <int, String>{
      0: 'Every time',
      30: '30 seconds',
      60: '1 minute',
      120: '2 minutes',
      300: '5 minutes',
      900: '15 minutes',
      1800: '30 minutes',
      3600: '1 hour',
    };

    final selected = await showDialog<int>(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: const Text('Re-verification Interval'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Enter a custom value in seconds, or pick a preset:',
                style: TextStyle(fontSize: 13),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: controller,
                keyboardType: TextInputType.number,
                autofocus: true,
                decoration: const InputDecoration(
                  labelText: 'Seconds',
                  border: OutlineInputBorder(),
                  hintText: 'e.g. 90',
                  suffixText: 'sec',
                ),
              ),
              const SizedBox(height: 16),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: presets.entries.map((e) {
                  final isSelected = appState.reverificationInterval == e.key;
                  return ChoiceChip(
                    label: Text(e.value,
                        style: const TextStyle(fontSize: 12)),
                    selected: isSelected,
                    onSelected: (_) {
                      Navigator.pop(ctx, e.key);
                    },
                  );
                }).toList(),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () {
                final val = int.tryParse(controller.text.trim());
                if (val == null || val < 0) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    const SnackBar(
                      content: Text('Enter a valid number (0 or more)'),
                    ),
                  );
                  return;
                }
                Navigator.pop(ctx, val);
              },
              child: const Text('Apply'),
            ),
          ],
        );
      },
    );

    if (selected != null) {
      await appState.setReverificationInterval(selected);
    }
  }

  Future<void> _showChangePinDialog(BuildContext context) async {
    final currentPinController = TextEditingController();
    final newPinController = TextEditingController();
    final confirmPinController = TextEditingController();

    await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Change PIN'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: currentPinController,
              keyboardType: TextInputType.number,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Current PIN',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: newPinController,
              keyboardType: TextInputType.number,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'New PIN (6+ digits)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: confirmPinController,
              keyboardType: TextInputType.number,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: 'Confirm New PIN',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () async {
              final currentOk = await PlatformService.verifyPin(
                currentPinController.text,
              );
              if (!currentOk) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    const SnackBar(content: Text('Current PIN is incorrect')),
                  );
                }
                return;
              }

              if (newPinController.text.length < 6) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    const SnackBar(
                        content: Text('New PIN must be at least 6 digits')),
                  );
                }
                return;
              }

              if (newPinController.text != confirmPinController.text) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    const SnackBar(content: Text('PINs do not match')),
                  );
                }
                return;
              }

              await PlatformService.setPin(newPinController.text);
              if (ctx.mounted) {
                Navigator.pop(ctx);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('PIN updated successfully')),
                );
              }
            },
            child: const Text('Update PIN'),
          ),
        ],
      ),
    );
  }

  void _showNudgeThresholdPicker(BuildContext context, AppState appState) {
    final options = [
      {'label': 'Disabled', 'value': 0},
      {'label': '3 attempts / hour', 'value': 3},
      {'label': '5 attempts / hour', 'value': 5},
      {'label': '8 attempts / hour', 'value': 8},
      {'label': '10 attempts / hour', 'value': 10},
      {'label': '15 attempts / hour', 'value': 15},
    ];

    showDialog(
      context: context,
      builder: (ctx) => SimpleDialog(
        title: const Text('Nudge Threshold'),
        children: options.map((opt) {
          final val = opt['value'] as int;
          return RadioListTile<int>(
            title: Text(opt['label'] as String),
            value: val,
            groupValue: appState.nudgeThreshold,
            onChanged: (v) {
              if (v != null) {
                appState.setNudgeThreshold(v);
                Navigator.pop(ctx);
              }
            },
          );
        }).toList(),
      ),
    );
  }
}
