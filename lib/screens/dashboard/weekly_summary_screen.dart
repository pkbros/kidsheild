import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/app_state.dart';

/// Weekly Summary screen showing 7-day trends for block attempts,
/// screen time, streaks, and parent screen time.
class WeeklySummaryScreen extends StatelessWidget {
  const WeeklySummaryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, _) {
        final data = appState.weeklySummary;

        return Scaffold(
          appBar: AppBar(
            title: const Text('Weekly Summary'),
            centerTitle: true,
          ),
          body: data.isEmpty
              ? const Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.bar_chart,
                          size: 64, color: Colors.grey),
                      SizedBox(height: 16),
                      Text(
                        'No data yet',
                        style: TextStyle(
                            fontSize: 18, color: Colors.grey),
                      ),
                      SizedBox(height: 8),
                      Text(
                        'Analytics data will appear\nafter a day of use.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: Colors.grey),
                      ),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: () => appState.refreshAnalytics(),
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // Title row
                      Text(
                        'Last 7 Days',
                        style: Theme.of(context)
                            .textTheme
                            .titleLarge
                            ?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 16),

                      // Block Attempts Chart
                      _buildBarChartCard(
                        context,
                        title: 'Block Attempts',
                        icon: Icons.block,
                        color: Colors.red,
                        data: data,
                        valueKey: 'total_block_attempts',
                      ),
                      const SizedBox(height: 16),

                      // Verified Access Chart
                      _buildBarChartCard(
                        context,
                        title: 'Verified Access',
                        icon: Icons.check_circle,
                        color: Colors.green,
                        data: data,
                        valueKey: 'total_verified',
                      ),
                      const SizedBox(height: 16),

                      // Screen Time Chart
                      _buildBarChartCard(
                        context,
                        title: 'Screen Time (minutes)',
                        icon: Icons.schedule,
                        color: Colors.blue,
                        data: data,
                        valueKey: 'total_screen_time_minutes',
                      ),
                      const SizedBox(height: 16),

                      // Best Streak Chart
                      _buildBarChartCard(
                        context,
                        title: 'Best Streak (minutes)',
                        icon: Icons.local_fire_department,
                        color: Colors.orange,
                        data: data,
                        valueKey: 'best_streak_minutes',
                      ),
                      const SizedBox(height: 16),

                      // Parent Screen Time (if available)
                      if (_hasParentData(data))
                        _buildBarChartCard(
                          context,
                          title: 'Parent Screen Time (minutes)',
                          icon: Icons.person,
                          color: Colors.purple,
                          data: data,
                          valueKey: 'parent_screen_time_minutes',
                        ),

                      // Summary Stats
                      const SizedBox(height: 24),
                      _buildWeekSummaryStats(context, data),

                      const SizedBox(height: 24),
                    ],
                  ),
                ),
        );
      },
    );
  }

  bool _hasParentData(List<Map<String, dynamic>> data) {
    return data.any((d) => d['parent_screen_time_minutes'] != null);
  }

  Widget _buildBarChartCard(
    BuildContext context, {
    required String title,
    required IconData icon,
    required Color color,
    required List<Map<String, dynamic>> data,
    required String valueKey,
  }) {
    // Extract values
    final values = data.map((d) {
      final v = d[valueKey];
      return (v is int) ? v : 0;
    }).toList();

    final maxVal = values.isEmpty
        ? 1
        : values.reduce((a, b) => a > b ? a : b).clamp(1, double.maxFinite.toInt());

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
                Icon(icon, color: color, size: 20),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: color,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            SizedBox(
              height: 120,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: List.generate(data.length, (i) {
                  final value = values[i];
                  final fraction = value / maxVal;
                  final dateStr = (data[i]['date'] as String?) ?? '';
                  final dayLabel = _shortDayLabel(dateStr);

                  return Expanded(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 2),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Text(
                            '$value',
                            style: TextStyle(
                              fontSize: 10,
                              color: Colors.grey[600],
                            ),
                          ),
                          const SizedBox(height: 4),
                          Flexible(
                            child: FractionallySizedBox(
                              heightFactor: fraction.clamp(0.05, 1.0),
                              child: Container(
                                decoration: BoxDecoration(
                                  color: color.withValues(alpha: 0.7),
                                  borderRadius: const BorderRadius.vertical(
                                    top: Radius.circular(4),
                                  ),
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            dayLabel,
                            style: TextStyle(
                              fontSize: 10,
                              color: Colors.grey[500],
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildWeekSummaryStats(
      BuildContext context, List<Map<String, dynamic>> data) {
    final totalAttempts = data.fold<int>(
        0, (sum, d) => sum + ((d['total_block_attempts'] as int?) ?? 0));
    final totalVerified = data.fold<int>(
        0, (sum, d) => sum + ((d['total_verified'] as int?) ?? 0));
    final avgScreenTime = data.isEmpty
        ? 0
        : data.fold<int>(0,
                (sum, d) => sum + ((d['total_screen_time_minutes'] as int?) ?? 0)) ~/
            data.length;
    final avgStreak = data.isEmpty
        ? 0
        : data.fold<int>(0,
                (sum, d) => sum + ((d['best_streak_minutes'] as int?) ?? 0)) ~/
            data.length;

    return Card(
      elevation: 0,
      color: Colors.indigo.withValues(alpha: 0.05),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.indigo.withValues(alpha: 0.2)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Week at a Glance',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Colors.indigo[800],
              ),
            ),
            const SizedBox(height: 12),
            _summaryRow('Total Block Attempts', '$totalAttempts'),
            _summaryRow('Total Verified Access', '$totalVerified'),
            _summaryRow(
                'Avg Daily Screen Time', _formatDuration(avgScreenTime)),
            _summaryRow(
                'Avg Daily Best Streak', _formatDuration(avgStreak)),
          ],
        ),
      ),
    );
  }

  Widget _summaryRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label,
              style: TextStyle(fontSize: 13, color: Colors.grey[700])),
          Text(value,
              style: const TextStyle(
                  fontSize: 14, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  String _shortDayLabel(String dateStr) {
    // dateStr is YYYY-MM-DD
    try {
      final parts = dateStr.split('-');
      if (parts.length == 3) {
        final dt = DateTime(
          int.parse(parts[0]),
          int.parse(parts[1]),
          int.parse(parts[2]),
        );
        const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
        return days[dt.weekday - 1];
      }
    } catch (_) {}
    return dateStr.length >= 5 ? dateStr.substring(5) : dateStr;
  }

  static String _formatDuration(int totalMinutes) {
    if (totalMinutes <= 0) return '0m';
    final h = totalMinutes ~/ 60;
    final m = totalMinutes % 60;
    if (h == 0) return '${m}m';
    if (m == 0) return '${h}h';
    return '${h}h ${m}m';
  }
}
