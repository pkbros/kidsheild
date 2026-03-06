import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/app_state.dart';
import '../../services/platform_service.dart';

class DailyBudgetScreen extends StatefulWidget {
  const DailyBudgetScreen({super.key});

  @override
  State<DailyBudgetScreen> createState() => _DailyBudgetScreenState();
}

class _DailyBudgetScreenState extends State<DailyBudgetScreen> {
  Map<String, dynamic> _timerStatus = {};
  List<dynamic> _taskList = [];
  int _earnedMinutes = 0;
  int _pendingCount = 0;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadStatus();
  }

  Future<void> _loadStatus() async {
    final timer = await PlatformService.getTimerStatus();
    final taskStatus = await PlatformService.getTaskStatus();
    if (mounted) {
      setState(() {
        _timerStatus = timer;
        _taskList = (taskStatus['tasks'] as List?) ?? [];
        _earnedMinutes = (taskStatus['earnedMinutes'] as int?) ?? 0;
        _pendingCount = (taskStatus['pendingCount'] as int?) ?? 0;
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    final usedMinutes = (_timerStatus['usedTodayMinutes'] as int?) ?? 0;
    final limitMinutes = (_timerStatus['dailyLimitMinutes'] as int?) ?? 30;
    final bonusMinutes = (_timerStatus['bonusTodayMinutes'] as int?) ?? 0;
    final effectiveLimit = limitMinutes + bonusMinutes;
    final remaining = (effectiveLimit - usedMinutes).clamp(0, 9999);
    final progress = effectiveLimit > 0
        ? (usedMinutes / effectiveLimit).clamp(0.0, 1.0)
        : 0.0;
    final enabled = _timerStatus['enabled'] == true;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Daily Budget'),
        centerTitle: true,
      ),
      body: RefreshIndicator(
        onRefresh: _loadStatus,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // ─── Timer Circle ───
            Center(
              child: SizedBox(
                width: 200,
                height: 200,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    SizedBox(
                      width: 200,
                      height: 200,
                      child: CircularProgressIndicator(
                        value: enabled ? progress : 0,
                        strokeWidth: 12,
                        backgroundColor: Colors.grey[200],
                        valueColor: AlwaysStoppedAnimation(
                          remaining > 0
                              ? Colors.purple
                              : Colors.red,
                        ),
                      ),
                    ),
                    Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          enabled ? '$remaining' : '—',
                          style: TextStyle(
                            fontSize: 48,
                            fontWeight: FontWeight.bold,
                            color: remaining > 0
                                ? Colors.purple[800]
                                : Colors.red[800],
                          ),
                        ),
                        Text(
                          enabled ? 'minutes left' : 'Timer off',
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // ─── Usage Summary ───
            if (enabled)
              Card(
                elevation: 0,
                color: Colors.purple.withValues(alpha: 0.06),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: BorderSide(
                      color: Colors.purple.withValues(alpha: 0.2)),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      _buildMetric('Used', '$usedMinutes min', Colors.orange),
                      _buildMetric('Limit', '$limitMinutes min', Colors.blue),
                      _buildMetric(
                          'Earned', '+$bonusMinutes min', Colors.green),
                    ],
                  ),
                ),
              ),
            if (!enabled)
              Card(
                elevation: 0,
                color: Colors.grey.withValues(alpha: 0.1),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Padding(
                  padding: EdgeInsets.all(16),
                  child: Text(
                    'Daily time limit is not enabled.\nAsk a parent to enable it in Settings.',
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            const SizedBox(height: 24),

            // ─── Task List ───
            if (_taskList.isNotEmpty) ...[
              Row(
                children: [
                  const Text(
                    '🌟 Daily Tasks',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const Spacer(),
                  if (_pendingCount > 0)
                    Chip(
                      label: Text('$_pendingCount pending'),
                      backgroundColor:
                          Colors.purple.withValues(alpha: 0.1),
                      labelStyle: TextStyle(
                        fontSize: 12,
                        color: Colors.purple[700],
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              ..._taskList.map((task) {
                final t = Map<String, dynamic>.from(task as Map);
                final completed = t['completedToday'] == true;
                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  color: completed
                      ? Colors.green.withValues(alpha: 0.06)
                      : null,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                    side: BorderSide(
                      color: completed
                          ? Colors.green.withValues(alpha: 0.3)
                          : Colors.grey.withValues(alpha: 0.2),
                    ),
                  ),
                  child: ListTile(
                    leading: Icon(
                      completed
                          ? Icons.check_circle
                          : Icons.radio_button_unchecked,
                      color: completed ? Colors.green : Colors.grey,
                    ),
                    title: Text(
                      t['title'] as String? ?? '',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        decoration: completed
                            ? TextDecoration.lineThrough
                            : null,
                        color: completed ? Colors.green[800] : null,
                      ),
                    ),
                    subtitle: Text(
                      completed
                          ? '✅ Completed (+${t['bonusMinutes']} min)'
                          : '+${t['bonusMinutes']} min bonus',
                      style: TextStyle(
                        fontSize: 12,
                        color: completed
                            ? Colors.green[600]
                            : Colors.purple[600],
                      ),
                    ),
                  ),
                );
              }),
            ] else
              Center(
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 32),
                  child: Column(
                    children: [
                      Icon(Icons.task_alt,
                          size: 48, color: Colors.grey[300]),
                      const SizedBox(height: 8),
                      Text(
                        'No tasks assigned',
                        style: TextStyle(color: Colors.grey[500]),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetric(String label, String value, Color color) {
    return Column(
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            color: Colors.grey[600],
          ),
        ),
      ],
    );
  }
}
