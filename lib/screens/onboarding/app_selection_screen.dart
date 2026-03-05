import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/app_state.dart';
import '../../services/platform_service.dart';

class AppSelectionScreen extends StatefulWidget {
  /// If true, this is used during onboarding (shows "Finish Setup" button).
  /// If false, it's accessed from settings.
  final bool isOnboarding;

  const AppSelectionScreen({super.key, this.isOnboarding = true});

  @override
  State<AppSelectionScreen> createState() => _AppSelectionScreenState();
}

class _AppSelectionScreenState extends State<AppSelectionScreen> {
  List<Map<String, dynamic>> _apps = [];
  List<Map<String, dynamic>> _filteredApps = [];
  Set<String> _blockedPackages = {};
  bool _isLoading = true;
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    final apps = await PlatformService.getInstalledApps();
    final blocked = await PlatformService.getBlockedApps();

    setState(() {
      _apps = apps;
      _filteredApps = apps;
      _blockedPackages = blocked.toSet();
      _isLoading = false;
    });
  }

  void _filterApps(String query) {
    setState(() {
      if (query.isEmpty) {
        _filteredApps = _apps;
      } else {
        _filteredApps = _apps
            .where((app) => (app['appName'] as String)
                .toLowerCase()
                .contains(query.toLowerCase()))
            .toList();
      }
    });
  }

  void _toggleApp(String packageName) {
    setState(() {
      if (_blockedPackages.contains(packageName)) {
        _blockedPackages.remove(packageName);
      } else {
        _blockedPackages.add(packageName);
      }
    });
  }

  Future<void> _save() async {
    await PlatformService.setBlockedApps(_blockedPackages.toList());

    if (widget.isOnboarding) {
      // Mark setup as complete and enable protection
      if (!mounted) return;
      final appState = context.read<AppState>();
      await appState.setSetupComplete(true);
      await appState.setProtectionEnabled(true);
      await PlatformService.startMonitoringService();
      await appState.initialize();

      if (mounted) {
        Navigator.pushNamedAndRemoveUntil(
          context,
          '/dashboard',
          (route) => false,
        );
      }
    } else {
      if (!mounted) return;
      final appState = context.read<AppState>();
      await appState.refreshBlockedApps();
      if (mounted) Navigator.pop(context);
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.isOnboarding ? 'Select Apps to Block' : 'Manage Blocked Apps'),
        actions: [
          Center(
            child: Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Text(
                '${_blockedPackages.length} blocked',
                style: TextStyle(
                  color: Theme.of(context).colorScheme.primary,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          // Info header
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            color: Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.3),
            child: const Text(
              'Toggle apps you want to restrict. When your child opens a blocked app, they\'ll need your face or PIN to continue.',
              style: TextStyle(fontSize: 13),
            ),
          ),

          // Search bar
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              controller: _searchController,
              onChanged: _filterApps,
              decoration: InputDecoration(
                hintText: 'Search apps...',
                prefixIcon: const Icon(Icons.search),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                contentPadding: const EdgeInsets.symmetric(horizontal: 16),
              ),
            ),
          ),

          // App list
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _filteredApps.isEmpty
                    ? const Center(child: Text('No apps found'))
                    : ListView.builder(
                        itemCount: _filteredApps.length,
                        itemBuilder: (context, index) {
                          final app = _filteredApps[index];
                          final packageName = app['packageName'] as String;
                          final appName = app['appName'] as String;
                          final iconBase64 = app['iconBase64'] as String;
                          final isBlocked =
                              _blockedPackages.contains(packageName);

                          Widget? leading;
                          if (iconBase64.isNotEmpty) {
                            try {
                              final bytes = base64Decode(iconBase64);
                              leading = Image.memory(
                                Uint8List.fromList(bytes),
                                width: 40,
                                height: 40,
                              );
                            } catch (_) {
                              leading = const Icon(Icons.android, size: 40);
                            }
                          } else {
                            leading = const Icon(Icons.android, size: 40);
                          }

                          return ListTile(
                            leading: leading,
                            title: Text(
                              appName,
                              style: const TextStyle(
                                  fontWeight: FontWeight.w500),
                            ),
                            subtitle: Text(
                              packageName,
                              style: const TextStyle(fontSize: 11),
                            ),
                            trailing: Switch(
                              value: isBlocked,
                              onChanged: (_) => _toggleApp(packageName),
                              activeThumbColor:
                                  Theme.of(context).colorScheme.error,
                            ),
                          );
                        },
                      ),
          ),

          // Save button
          Padding(
            padding: const EdgeInsets.all(16),
            child: SizedBox(
              width: double.infinity,
              height: 56,
              child: FilledButton(
                onPressed: _save,
                child: Text(
                  widget.isOnboarding ? 'Finish Setup & Activate' : 'Save Changes',
                  style: const TextStyle(fontSize: 16),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
