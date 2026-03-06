import 'package:flutter/material.dart';
import '../services/platform_service.dart';

/// Central state management for KidShield app.
class AppState extends ChangeNotifier {
  bool _isSetupComplete = false;
  bool _isProtectionEnabled = false;
  int _reverificationInterval = 1800; // seconds
  bool _isDebugMode = false;
  bool _isMascotEnabled = true;
  String _overlayMode = 'video'; // "video", "buddy", or "classic"
  List<String> _blockedApps = [];
  List<String> _registeredFaces = [];
  bool _isLoading = true;

  // Detection mode
  Map<String, dynamic> _detectionMode = {};

  // Analytics state
  Map<String, dynamic> _todayStats = {};
  Map<String, dynamic> _yesterdayStats = {};
  Map<String, dynamic> _currentStreak = {};
  List<Map<String, dynamic>> _weeklySummary = [];
  String _buddyStatusMessage = 'Keep up the great work!';
  bool _isParentTrackingEnabled = false;
  String? _parentInsight;
  int _nudgeThreshold = 5;

  // Earn-time / daily timer state
  Map<String, dynamic> _timerStatus = {};
  Map<String, dynamic> _taskStatus = {};
  List<Map<String, dynamic>> _tasks = [];

  bool get isSetupComplete => _isSetupComplete;
  bool get isProtectionEnabled => _isProtectionEnabled;
  int get reverificationInterval => _reverificationInterval;
  bool get isDebugMode => _isDebugMode;
  bool get isMascotEnabled => _isMascotEnabled;
  String get overlayMode => _overlayMode;
  List<String> get blockedApps => _blockedApps;
  List<String> get registeredFaces => _registeredFaces;
  bool get isLoading => _isLoading;

  // Detection mode getters
  Map<String, dynamic> get detectionMode => _detectionMode;
  String get detectionModeLabel {
    final mode = _detectionMode['mode'] as String? ?? 'none';
    switch (mode) {
      case 'hybrid':
        return 'Hybrid (Instant + Polling)';
      case 'polling':
        return 'UsageStats Polling (~300ms)';
      case 'accessibility':
        return 'Accessibility (Instant)';
      default:
        return 'Not Active';
    }
  }

  // Analytics getters
  Map<String, dynamic> get todayStats => _todayStats;
  Map<String, dynamic> get yesterdayStats => _yesterdayStats;
  Map<String, dynamic> get currentStreak => _currentStreak;
  List<Map<String, dynamic>> get weeklySummary => _weeklySummary;
  String get buddyStatusMessage => _buddyStatusMessage;
  bool get isParentTrackingEnabled => _isParentTrackingEnabled;
  String? get parentInsight => _parentInsight;
  int get nudgeThreshold => _nudgeThreshold;

  // Earn-time getters
  Map<String, dynamic> get timerStatus => _timerStatus;
  Map<String, dynamic> get taskStatus => _taskStatus;
  List<Map<String, dynamic>> get tasks => _tasks;
  bool get isTimerEnabled => _timerStatus['enabled'] == true;
  int get dailyLimitMinutes => (_timerStatus['dailyLimitMinutes'] as int?) ?? 30;
  int get usedTodayMinutes => (_timerStatus['usedTodayMinutes'] as int?) ?? 0;
  int get remainingMinutes => (_timerStatus['remainingMinutes'] as int?) ?? 30;
  int get earnedTodayMinutes => (_taskStatus['earnedMinutes'] as int?) ?? 0;
  int get pendingTaskCount => (_taskStatus['pendingCount'] as int?) ?? 0;

  Future<void> initialize() async {
    try {
      _isSetupComplete = await PlatformService.isSetupComplete();
      _isProtectionEnabled = await PlatformService.isProtectionEnabled();
      _reverificationInterval = await PlatformService.getReverificationInterval();
      _isDebugMode = await PlatformService.isDebugMode();
      _isMascotEnabled = await PlatformService.isMascotEnabled();
      _overlayMode = await PlatformService.getOverlayMode();
      _blockedApps = await PlatformService.getBlockedApps();
      _registeredFaces = await PlatformService.getRegisteredFaces();

      // Load detection mode
      _detectionMode = await PlatformService.getDetectionMode();

      // Load analytics data
      await refreshAnalytics();
    } catch (e) {
      debugPrint('AppState.initialize() error: $e');
      // Proceed with defaults so the app doesn't get stuck on loading
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Refresh all analytics data from the platform.
  Future<void> refreshAnalytics() async {
    try {
      _detectionMode = await PlatformService.getDetectionMode();
      _todayStats = await PlatformService.getTodayStats();
      _yesterdayStats = await PlatformService.getYesterdayStats();
      _currentStreak = await PlatformService.getCurrentStreakInfo();
      _weeklySummary = await PlatformService.getWeeklySummary();
      _buddyStatusMessage = await PlatformService.getBuddyStatusMessage();
      _isParentTrackingEnabled = await PlatformService.isParentTrackingEnabled();
      _parentInsight = await PlatformService.getParentInsight();
      _nudgeThreshold = await PlatformService.getNudgeThreshold();
      _timerStatus = await PlatformService.getTimerStatus();
      _taskStatus = await PlatformService.getTaskStatus();
      _tasks = await PlatformService.getTasks();
      notifyListeners();
    } catch (e) {
      debugPrint('AppState.refreshAnalytics() error: $e');
    }
  }

  Future<void> setSetupComplete(bool complete) async {
    await PlatformService.setSetupComplete(complete);
    _isSetupComplete = complete;
    notifyListeners();
  }

  Future<void> setProtectionEnabled(bool enabled) async {
    await PlatformService.setProtectionEnabled(enabled);
    _isProtectionEnabled = enabled;
    notifyListeners();
  }

  Future<void> setReverificationInterval(int seconds) async {
    await PlatformService.setReverificationInterval(seconds);
    _reverificationInterval = seconds;
    notifyListeners();
  }

  Future<void> setDebugMode(bool enabled) async {
    await PlatformService.setDebugMode(enabled);
    _isDebugMode = enabled;
    notifyListeners();
  }

  Future<void> setMascotEnabled(bool enabled) async {
    await PlatformService.setMascotEnabled(enabled);
    _isMascotEnabled = enabled;
    notifyListeners();
  }

  Future<void> setOverlayMode(String mode) async {
    await PlatformService.setOverlayMode(mode);
    _overlayMode = mode;
    _isMascotEnabled = mode != 'classic';
    notifyListeners();
  }

  Future<void> setBlockedApps(List<String> apps) async {
    await PlatformService.setBlockedApps(apps);
    _blockedApps = apps;
    notifyListeners();
  }

  Future<void> refreshRegisteredFaces() async {
    _registeredFaces = await PlatformService.getRegisteredFaces();
    notifyListeners();
  }

  Future<void> refreshBlockedApps() async {
    _blockedApps = await PlatformService.getBlockedApps();
    notifyListeners();
  }

  // ─── Parent Self-Awareness ───

  Future<void> setParentTrackingEnabled(bool enabled) async {
    await PlatformService.setParentTrackingEnabled(enabled);
    _isParentTrackingEnabled = enabled;
    notifyListeners();
  }

  Future<void> setNudgeThreshold(int threshold) async {
    await PlatformService.setNudgeThreshold(threshold);
    _nudgeThreshold = threshold;
    notifyListeners();
  }

  // ─── Earn-Time / Daily Timer ───

  Future<void> setTimerEnabled(bool enabled) async {
    await PlatformService.setTimerEnabled(enabled);
    _timerStatus = await PlatformService.getTimerStatus();
    notifyListeners();
  }

  Future<void> setDailyLimit(int minutes) async {
    await PlatformService.setDailyLimit(minutes);
    _timerStatus = await PlatformService.getTimerStatus();
    notifyListeners();
  }

  Future<void> refreshTasks() async {
    _tasks = await PlatformService.getTasks();
    _taskStatus = await PlatformService.getTaskStatus();
    _timerStatus = await PlatformService.getTimerStatus();
    notifyListeners();
  }
}
