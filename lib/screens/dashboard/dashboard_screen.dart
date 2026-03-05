import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/app_state.dart';
import '../../services/platform_service.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen>
    with WidgetsBindingObserver {
  bool _serviceRunning = false;
  int _versionTapCount = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _checkServiceStatus();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _checkServiceStatus();
      context.read<AppState>().refreshAnalytics();
    }
  }

  Future<void> _checkServiceStatus() async {
    final running = await PlatformService.isMonitoringServiceRunning();
    if (mounted) setState(() => _serviceRunning = running);
  }

  Future<void> _navigateToSettings() async {
    final verified = await _verifyAccess();
    if (verified && mounted) {
      Navigator.pushNamed(context, '/settings');
    }
  }

  Future<bool> _verifyAccess() async {
    final pin = await showDialog<String>(
      context: context,
      builder: (ctx) {
        final controller = TextEditingController();
        return AlertDialog(
          title: const Text('Parent Verification'),
          content: TextField(
            controller: controller,
            keyboardType: TextInputType.number,
            obscureText: true,
            decoration: const InputDecoration(
              labelText: 'Enter PIN',
              border: OutlineInputBorder(),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, controller.text),
              child: const Text('Verify'),
            ),
          ],
        );
      },
    );

    if (pin == null || pin.isEmpty) return false;
    return await PlatformService.verifyPin(pin);
  }

  void _onVersionTap() {
    _versionTapCount++;
    if (_versionTapCount >= 7) {
      _versionTapCount = 0;
      _toggleDebugMode();
    }
  }

  Future<void> _toggleDebugMode() async {
    final appState = context.read<AppState>();
    final newValue = !appState.isDebugMode;
    await appState.setDebugMode(newValue);

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            newValue ? 'Debug mode enabled' : 'Debug mode disabled',
          ),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, _) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('KidShield'),
            centerTitle: true,
            actions: [
              IconButton(
                icon: const Icon(Icons.settings),
                onPressed: _navigateToSettings,
              ),
            ],
          ),
          body: RefreshIndicator(
            onRefresh: () async {
              await appState.initialize();
              await _checkServiceStatus();
            },
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // Protection status card
                _buildStatusCard(context, appState),
                const SizedBox(height: 16),

                // Buddy status message
                _buildBuddyStatusCard(context, appState),
                const SizedBox(height: 16),

                // Daily Report Card
                _buildReportCard(context, appState),
                const SizedBox(height: 16),

                // Current Streak
                _buildStreakCard(context, appState),
                const SizedBox(height: 16),

                // Screen Time
                _buildScreenTimeCard(context, appState),
                const SizedBox(height: 16),

                // Top Blocked Apps
                _buildTopBlockedAppsCard(context, appState),
                const SizedBox(height: 16),

                // Quick Actions
                Text(
                  'Quick Actions',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                ),
                const SizedBox(height: 12),
                _buildActionTile(
                  context,
                  icon: Icons.apps,
                  title: 'Manage Blocked Apps',
                  subtitle: 'Add or remove apps from the block list',
                  onTap: () async {
                    if (await _verifyAccess()) {
                      if (mounted) {
                        Navigator.pushNamed(context, '/manage-apps');
                      }
                    }
                  },
                ),
                _buildActionTile(
                  context,
                  icon: Icons.face,
                  title: 'Manage Faces',
                  subtitle: 'Add or remove authorized guardian faces',
                  onTap: () async {
                    if (await _verifyAccess()) {
                      if (mounted) {
                        Navigator.pushNamed(context, '/manage-faces');
                      }
                    }
                  },
                ),
                _buildActionTile(
                  context,
                  icon: Icons.bar_chart,
                  title: 'Weekly Summary',
                  subtitle: 'View 7-day trends and patterns',
                  onTap: () async {
                    if (await _verifyAccess()) {
                      if (mounted) {
                        Navigator.pushNamed(context, '/weekly-summary');
                      }
                    }
                  },
                ),
                _buildActionTile(
                  context,
                  icon: Icons.health_and_safety,
                  title: 'Permission Health Check',
                  subtitle: 'Verify all required permissions are active',
                  onTap: () {
                    Navigator.pushNamed(context, '/permission-check');
                  },
                ),

                if (appState.isDebugMode) ...[
                  const SizedBox(height: 24),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.yellow.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.orange),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          '🛠 Debug Mode',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 8),
                        TextButton(
                          onPressed: () async {
                            await PlatformService.clearVerificationSessions();
                            if (mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content:
                                      Text('Verification sessions cleared'),
                                ),
                              );
                            }
                          },
                          child: const Text('Clear All Verification Sessions'),
                        ),
                        TextButton(
                          onPressed: () async {
                            await appState.setReverificationInterval(0);
                            if (mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text(
                                      'Interval set to "Every time" (testing)'),
                                ),
                              );
                            }
                          },
                          child: const Text(
                              'Set Interval to "Every Time" (for testing)'),
                        ),
                      ],
                    ),
                  ),
                ],

                // Version (7-tap to toggle debug mode)
                const SizedBox(height: 32),
                GestureDetector(
                  onTap: _onVersionTap,
                  child: Center(
                    child: Text(
                      'KidShield v1.5.0',
                      style: TextStyle(
                        color: Colors.grey[400],
                        fontSize: 12,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  // ────────────────────────── Protection Status ──────────────────────────

  Widget _buildStatusCard(BuildContext context, AppState appState) {
    final isActive = appState.isProtectionEnabled && _serviceRunning;
    final detectionMode = appState.detectionMode;
    final modeStr = appState.detectionModeLabel;

    return Card(
      elevation: 0,
      color: isActive
          ? Colors.green.withValues(alpha: 0.1)
          : Colors.red.withValues(alpha: 0.1),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: isActive
              ? Colors.green.withValues(alpha: 0.3)
              : Colors.red.withValues(alpha: 0.3),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          children: [
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                color: isActive ? Colors.green : Colors.red,
                shape: BoxShape.circle,
              ),
              child: Icon(
                isActive ? Icons.shield : Icons.shield_outlined,
                color: Colors.white,
                size: 28,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    isActive ? 'Protection Active' : 'Protection Disabled',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: isActive ? Colors.green[800] : Colors.red[800],
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    isActive
                        ? '${appState.blockedApps.length} apps blocked · ${appState.registeredFaces.length} faces'
                        : 'Tap settings to activate protection',
                    style: TextStyle(
                      fontSize: 13,
                      color: isActive ? Colors.green[600] : Colors.red[600],
                    ),
                  ),
                  if (isActive) ...[
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Icon(
                          detectionMode['mode'] == 'hybrid'
                              ? Icons.bolt
                              : Icons.poll,
                          size: 14,
                          color: Colors.green[400],
                        ),
                        const SizedBox(width: 4),
                        Text(
                          modeStr,
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.green[400],
                          ),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ────────────────────────── Buddy Status ──────────────────────────

  Widget _buildBuddyStatusCard(BuildContext context, AppState appState) {
    return Card(
      elevation: 0,
      color: Colors.amber.withValues(alpha: 0.08),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.amber.withValues(alpha: 0.3)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Text('🐾', style: TextStyle(fontSize: 28)),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                appState.buddyStatusMessage,
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.amber[900],
                  fontStyle: FontStyle.italic,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // ────────────────────────── Daily Report Card ──────────────────────────

  Widget _buildReportCard(BuildContext context, AppState appState) {
    final today = appState.todayStats;
    final yesterday = appState.yesterdayStats;

    final attempts = (today['total_block_attempts'] as int?) ?? 0;
    final verified = (today['total_verified'] as int?) ?? 0;
    final yesterdayAttempts =
        (yesterday['total_block_attempts'] as int?) ?? 0;
    final yesterdayVerified =
        (yesterday['total_verified'] as int?) ?? 0;

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.assignment, color: Colors.indigo[600], size: 22),
                const SizedBox(width: 8),
                Text(
                  "Today's Report Card",
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.indigo[800],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: _buildReportMetric(
                    label: 'Block Attempts',
                    value: '$attempts',
                    icon: Icons.block,
                    color: Colors.red,
                    trend: _trendIndicator(attempts, yesterdayAttempts,
                        invertGood: true),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildReportMetric(
                    label: 'Verified Access',
                    value: '$verified',
                    icon: Icons.check_circle,
                    color: Colors.green,
                    trend: _trendIndicator(verified, yesterdayVerified),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildReportMetric({
    required String label,
    required String value,
    required IconData icon,
    required Color color,
    required Widget trend,
  }) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        children: [
          Icon(icon, color: color, size: 24),
          const SizedBox(height: 6),
          Text(
            value,
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: TextStyle(fontSize: 11, color: Colors.grey[600]),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 4),
          trend,
        ],
      ),
    );
  }

  /// Build a trend indicator comparing today vs yesterday.
  /// [invertGood] = true when lower is better (e.g., block attempts).
  Widget _trendIndicator(int today, int yesterday,
      {bool invertGood = false}) {
    if (yesterday == 0 && today == 0) {
      return Text('—',
          style: TextStyle(fontSize: 11, color: Colors.grey[400]));
    }
    final diff = today - yesterday;
    if (diff == 0) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.remove, size: 12, color: Colors.grey[400]),
          Text(' same',
              style: TextStyle(fontSize: 11, color: Colors.grey[500])),
        ],
      );
    }
    final isUp = diff > 0;
    final isGood = invertGood ? !isUp : isUp;
    final color = isGood ? Colors.green : Colors.orange;
    final arrow = isUp ? Icons.arrow_upward : Icons.arrow_downward;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(arrow, size: 12, color: color),
        Text(
          ' ${diff.abs()} vs yesterday',
          style: TextStyle(fontSize: 11, color: color),
        ),
      ],
    );
  }

  // ────────────────────────── Streak Card ──────────────────────────

  Widget _buildStreakCard(BuildContext context, AppState appState) {
    final streak = appState.currentStreak;
    final currentMinutes = (streak['current_minutes'] as int?) ?? 0;
    final todayBest = (streak['today_best_minutes'] as int?) ?? 0;
    final longestEver = (streak['longest_ever_minutes'] as int?) ?? 0;
    final isActive = (streak['is_active'] as bool?) ?? false;

    return Card(
      elevation: 0,
      color: Colors.teal.withValues(alpha: 0.06),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.teal.withValues(alpha: 0.25)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.local_fire_department,
                    color: Colors.orange[600], size: 22),
                const SizedBox(width: 8),
                Text(
                  'Screen-Free Streak',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.teal[800],
                  ),
                ),
                const Spacer(),
                if (isActive)
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: Colors.green.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Text(
                      'ACTIVE',
                      style: TextStyle(
                        fontSize: 10,
                        color: Colors.green,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            Center(
              child: Text(
                _formatDuration(currentMinutes),
                style: TextStyle(
                  fontSize: 36,
                  fontWeight: FontWeight.bold,
                  color: Colors.teal[700],
                ),
              ),
            ),
            const SizedBox(height: 4),
            Center(
              child: Text(
                'current streak',
                style: TextStyle(fontSize: 12, color: Colors.teal[400]),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildStreakStat(
                    "Today's Best", _formatDuration(todayBest)),
                Container(
                    width: 1,
                    height: 30,
                    color: Colors.teal.withValues(alpha: 0.2)),
                _buildStreakStat(
                    'Longest Ever', _formatDuration(longestEver)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStreakStat(String label, String value) {
    return Column(
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: Colors.teal[700],
          ),
        ),
        const SizedBox(height: 2),
        Text(label,
            style: TextStyle(fontSize: 11, color: Colors.teal[400])),
      ],
    );
  }

  // ────────────────────────── Screen Time Card ──────────────────────────

  Widget _buildScreenTimeCard(BuildContext context, AppState appState) {
    final today = appState.todayStats;
    final childMinutes =
        (today['total_screen_time_minutes'] as int?) ?? 0;
    final parentMinutes = today['parent_screen_time_minutes'] as int?;
    final parentInsight = appState.parentInsight;

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.schedule, color: Colors.blue[600], size: 22),
                const SizedBox(width: 8),
                Text(
                  'Screen Time Today',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.blue[800],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _buildScreenTimeBadge(
                    label: 'Child',
                    minutes: childMinutes,
                    color: Colors.blue,
                    icon: Icons.child_care,
                  ),
                ),
                if (parentMinutes != null) ...[
                  const SizedBox(width: 12),
                  Expanded(
                    child: _buildScreenTimeBadge(
                      label: 'You',
                      minutes: parentMinutes,
                      color: Colors.purple,
                      icon: Icons.person,
                    ),
                  ),
                ],
              ],
            ),
            if (parentInsight != null) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.orange.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    const Text('💡', style: TextStyle(fontSize: 16)),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        parentInsight,
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.orange[800],
                          fontStyle: FontStyle.italic,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildScreenTimeBadge({
    required String label,
    required int minutes,
    required Color color,
    required IconData icon,
  }) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        children: [
          Icon(icon, color: color, size: 22),
          const SizedBox(height: 6),
          Text(
            _formatDuration(minutes),
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
          const SizedBox(height: 2),
          Text(label,
              style: TextStyle(fontSize: 12, color: Colors.grey[600])),
        ],
      ),
    );
  }

  // ────────────────────────── Top Blocked Apps ──────────────────────────

  Widget _buildTopBlockedAppsCard(BuildContext context, AppState appState) {
    final today = appState.todayStats;
    final topApps = today['top_blocked_apps'];
    final topAppsList = (topApps is List)
        ? topApps
            .map((e) => Map<String, dynamic>.from(e as Map))
            .toList()
        : <Map<String, dynamic>>[];

    if (topAppsList.isEmpty) {
      return const SizedBox.shrink();
    }

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.withValues(alpha: 0.2)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.trending_up, color: Colors.red[400], size: 22),
                const SizedBox(width: 8),
                Text(
                  'Most Attempted Today',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.red[800],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ...topAppsList.map(
              (app) => _buildTopAppRow(
                app['app_package'] as String,
                (app['attempts'] as int?) ?? 0,
                (app['verified'] as int?) ?? 0,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopAppRow(String packageName, int attempts, int verified) {
    final shortName = packageName.split('.').last;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: Colors.red.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(Icons.android, size: 18, color: Colors.red),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              shortName,
              style: const TextStyle(fontSize: 14),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          Text(
            '$attempts attempt${attempts == 1 ? '' : 's'}',
            style: TextStyle(
              fontSize: 12,
              color: Colors.red[400],
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(width: 8),
          Text(
            '$verified ✓',
            style: TextStyle(
              fontSize: 12,
              color: Colors.green[400],
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  // ────────────────────────── Quick Action Tiles ──────────────────────────

  Widget _buildActionTile(
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
        leading: CircleAvatar(
          backgroundColor:
              Theme.of(context).colorScheme.primaryContainer,
          child: Icon(icon,
              color: Theme.of(context).colorScheme.primary),
        ),
        title: Text(title,
            style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 12)),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }

  // ────────────────────────── Helpers ──────────────────────────

  static String _formatDuration(int totalMinutes) {
    if (totalMinutes <= 0) return '0m';
    final h = totalMinutes ~/ 60;
    final m = totalMinutes % 60;
    if (h == 0) return '${m}m';
    if (m == 0) return '${h}h';
    return '${h}h ${m}m';
  }
}
