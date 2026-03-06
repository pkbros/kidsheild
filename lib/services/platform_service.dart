import 'package:flutter/services.dart';

/// Bridge to native Android platform (Kotlin) via MethodChannel.
/// Handles permissions, services, face engine, blocked apps, PIN, settings.
class PlatformService {
  static const _channel = MethodChannel('com.kidshield.kid_shield/platform');

  // ─── Permissions ───

  static Future<bool> checkAccessibilityPermission() async {
    return await _channel.invokeMethod('checkAccessibilityPermission') ?? false;
  }

  static Future<void> requestAccessibilityPermission() async {
    await _channel.invokeMethod('requestAccessibilityPermission');
  }

  static Future<bool> checkOverlayPermission() async {
    return await _channel.invokeMethod('checkOverlayPermission') ?? false;
  }

  static Future<void> requestOverlayPermission() async {
    await _channel.invokeMethod('requestOverlayPermission');
  }

  static Future<bool> checkDeviceAdminPermission() async {
    return await _channel.invokeMethod('checkDeviceAdminPermission') ?? false;
  }

  static Future<void> requestDeviceAdminPermission() async {
    await _channel.invokeMethod('requestDeviceAdminPermission');
  }

  static Future<bool> checkBatteryOptimization() async {
    return await _channel.invokeMethod('checkBatteryOptimization') ?? false;
  }

  static Future<void> requestBatteryOptimization() async {
    await _channel.invokeMethod('requestBatteryOptimization');
  }

  static Future<bool> checkUsageStatsPermission() async {
    return await _channel.invokeMethod('checkUsageStatsPermission') ?? false;
  }

  static Future<void> requestUsageStatsPermission() async {
    await _channel.invokeMethod('requestUsageStatsPermission');
  }

  // ─── Service Control ───

  static Future<void> startMonitoringService() async {
    await _channel.invokeMethod('startMonitoringService');
  }

  static Future<void> stopMonitoringService() async {
    await _channel.invokeMethod('stopMonitoringService');
  }

  static Future<bool> isMonitoringServiceRunning() async {
    return await _channel.invokeMethod('isMonitoringServiceRunning') ?? false;
  }

  // ─── Installed Apps ───

  static Future<List<Map<String, dynamic>>> getInstalledApps() async {
    final result = await _channel.invokeMethod('getInstalledApps');
    if (result == null) return [];
    return (result as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  // ─── Blocked Apps ───

  static Future<List<String>> getBlockedApps() async {
    final result = await _channel.invokeMethod('getBlockedApps');
    if (result == null) return [];
    return (result as List).cast<String>();
  }

  static Future<void> setBlockedApps(List<String> apps) async {
    await _channel.invokeMethod('setBlockedApps', {'apps': apps});
  }

  // ─── PIN ───

  static Future<void> setPin(String pin) async {
    await _channel.invokeMethod('setPin', {'pin': pin});
  }

  static Future<bool> verifyPin(String pin) async {
    return await _channel.invokeMethod('verifyPin', {'pin': pin}) ?? false;
  }

  static Future<bool> isPinSet() async {
    return await _channel.invokeMethod('isPinSet') ?? false;
  }

  // ─── Face Registration ───

  static Future<void> initFaceEngine() async {
    await _channel.invokeMethod('initFaceEngine');
  }

  static Future<Map<String, dynamic>> processAndRegisterFace(
    Uint8List imageBytes,
    String name,
  ) async {
    final result = await _channel.invokeMethod('processAndRegisterFace', {
      'imageBytes': imageBytes,
      'name': name,
    });
    return Map<String, dynamic>.from(result as Map);
  }

  static Future<List<String>> getRegisteredFaces() async {
    final result = await _channel.invokeMethod('getRegisteredFaces');
    if (result == null) return [];
    return (result as List).cast<String>();
  }

  static Future<void> deleteRegisteredFace(int index) async {
    await _channel.invokeMethod('deleteRegisteredFace', {'index': index});
  }

  static Future<Map<String, dynamic>> verifyFace(Uint8List imageBytes) async {
    final result = await _channel.invokeMethod('verifyFace', {
      'imageBytes': imageBytes,
    });
    return Map<String, dynamic>.from(result as Map);
  }

  // ─── Settings ───

  static Future<int> getReverificationInterval() async {
    return await _channel.invokeMethod('getReverificationInterval') ?? 1800;
  }

  /// Set re-verification interval in seconds. 0 = every time.
  static Future<void> setReverificationInterval(int seconds) async {
    await _channel.invokeMethod('setReverificationInterval', {
      'interval': seconds,
    });
  }

  static Future<bool> isProtectionEnabled() async {
    return await _channel.invokeMethod('isProtectionEnabled') ?? false;
  }

  static Future<void> setProtectionEnabled(bool enabled) async {
    await _channel.invokeMethod('setProtectionEnabled', {'enabled': enabled});
  }

  static Future<bool> isSetupComplete() async {
    return await _channel.invokeMethod('isSetupComplete') ?? false;
  }

  static Future<void> setSetupComplete(bool complete) async {
    await _channel.invokeMethod('setSetupComplete', {'complete': complete});
  }

  static Future<bool> isDebugMode() async {
    return await _channel.invokeMethod('isDebugMode') ?? false;
  }

  static Future<void> setDebugMode(bool enabled) async {
    await _channel.invokeMethod('setDebugMode', {'enabled': enabled});
  }

  static Future<void> clearVerificationSessions() async {
    await _channel.invokeMethod('clearVerificationSessions');
  }

  // ─── Mascot / Buddy ───

  static Future<bool> isMascotEnabled() async {
    return await _channel.invokeMethod('isMascotEnabled') ?? true;
  }

  static Future<void> setMascotEnabled(bool enabled) async {
    await _channel.invokeMethod('setMascotEnabled', {'enabled': enabled});
  }

  /// Get overlay mode: "video", "buddy", or "classic"
  static Future<String> getOverlayMode() async {
    return await _channel.invokeMethod('getOverlayMode') ?? 'video';
  }

  /// Set overlay mode: "video", "buddy", or "classic"
  static Future<void> setOverlayMode(String mode) async {
    await _channel.invokeMethod('setOverlayMode', {'mode': mode});
  }

  // ─── Analytics / Usage Tracking ───

  /// Get today's usage stats (block attempts, verified, screen time, streak, top apps).
  static Future<Map<String, dynamic>> getTodayStats() async {
    final result = await _channel.invokeMethod('getTodayStats');
    if (result == null) return {};
    return Map<String, dynamic>.from(result as Map);
  }

  /// Get yesterday's usage stats for trend comparison.
  static Future<Map<String, dynamic>> getYesterdayStats() async {
    final result = await _channel.invokeMethod('getYesterdayStats');
    if (result == null) return {};
    return Map<String, dynamic>.from(result as Map);
  }

  /// Get weekly summary (last 7 days of daily aggregates).
  static Future<List<Map<String, dynamic>>> getWeeklySummary() async {
    final result = await _channel.invokeMethod('getWeeklySummary');
    if (result == null) return [];
    return (result as List)
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
  }

  /// Get current streak info (current_minutes, is_active, today_best, longest_ever).
  static Future<Map<String, dynamic>> getCurrentStreakInfo() async {
    final result = await _channel.invokeMethod('getCurrentStreakInfo');
    if (result == null) return {};
    return Map<String, dynamic>.from(result as Map);
  }

  /// Get block events for a specific date (YYYY-MM-DD).
  static Future<List<Map<String, dynamic>>> getBlockEventsForDate(
    String date,
  ) async {
    final result = await _channel.invokeMethod(
      'getBlockEventsForDate',
      {'date': date},
    );
    if (result == null) return [];
    return (result as List)
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
  }

  /// Get contextual Buddy status message for dashboard.
  static Future<String> getBuddyStatusMessage() async {
    return await _channel.invokeMethod('getBuddyStatusMessage') ??
        'Keep up the great work!';
  }

  /// Force refresh today's aggregate data.
  static Future<void> refreshTodayAggregate() async {
    await _channel.invokeMethod('refreshTodayAggregate');
  }

  // ─── Parent Self-Awareness ───

  static Future<bool> isParentTrackingEnabled() async {
    return await _channel.invokeMethod('isParentTrackingEnabled') ?? false;
  }

  static Future<void> setParentTrackingEnabled(bool enabled) async {
    await _channel.invokeMethod(
      'setParentTrackingEnabled',
      {'enabled': enabled},
    );
  }

  static Future<int> getParentScreenTimeGoal() async {
    return await _channel.invokeMethod('getParentScreenTimeGoal') ?? 0;
  }

  static Future<void> setParentScreenTimeGoal(int minutes) async {
    await _channel.invokeMethod(
      'setParentScreenTimeGoal',
      {'minutes': minutes},
    );
  }

  /// Get an optional parent insight message (null if nothing to say).
  static Future<String?> getParentInsight() async {
    return await _channel.invokeMethod('getParentInsight');
  }

  // ─── Nudge Settings ───

  static Future<int> getNudgeThreshold() async {
    return await _channel.invokeMethod('getNudgeThreshold') ?? 5;
  }

  static Future<void> setNudgeThreshold(int threshold) async {
    await _channel.invokeMethod('setNudgeThreshold', {'threshold': threshold});
  }

  // ─── Detection Mode ───

  /// Get the current detection mode and permission state.
  /// Returns: { mode: "hybrid"|"polling"|"accessibility"|"none",
  ///            usageStatsGranted: bool, accessibilityEnabled: bool,
  ///            pollingActive: bool }
  static Future<Map<String, dynamic>> getDetectionMode() async {
    final result = await _channel.invokeMethod('getDetectionMode');
    if (result == null) return {};
    return Map<String, dynamic>.from(result as Map);
  }

  // ─── Daily Timer ───

  /// Get full timer status map.
  static Future<Map<String, dynamic>> getTimerStatus() async {
    final result = await _channel.invokeMethod('getTimerStatus');
    if (result == null) return {};
    return Map<String, dynamic>.from(result as Map);
  }

  /// Enable or disable the daily timer.
  static Future<void> setTimerEnabled(bool enabled) async {
    await _channel.invokeMethod('setTimerEnabled', {'enabled': enabled});
  }

  /// Set daily limit in minutes.
  static Future<void> setDailyLimit(int minutes) async {
    await _channel.invokeMethod('setDailyLimit', {'minutes': minutes});
  }

  /// Get first-open-of-day status.
  static Future<Map<String, dynamic>> getFirstOpenStatus() async {
    final result = await _channel.invokeMethod('getFirstOpenStatus');
    if (result == null) return {};
    return Map<String, dynamic>.from(result as Map);
  }

  /// Mark first open video as shown today.
  static Future<void> markFirstOpenShown() async {
    await _channel.invokeMethod('markFirstOpenShown');
  }

  // ─── Earn-Time Tasks ───

  /// Get all parent-defined tasks.
  static Future<List<Map<String, dynamic>>> getTasks() async {
    final result = await _channel.invokeMethod('getTasks');
    if (result == null) return [];
    return (result as List)
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
  }

  /// Add a new task.
  static Future<Map<String, dynamic>> addTask({
    required String title,
    String description = '',
    int bonusMinutes = 0,
  }) async {
    final result = await _channel.invokeMethod('addTask', {
      'title': title,
      'description': description,
      'bonusMinutes': bonusMinutes,
    });
    return Map<String, dynamic>.from(result as Map);
  }

  /// Remove a task by ID.
  static Future<void> removeTask(String taskId) async {
    await _channel.invokeMethod('removeTask', {'taskId': taskId});
  }

  /// Update an existing task.
  static Future<void> updateTask({
    required String taskId,
    required String title,
    String description = '',
    int bonusMinutes = 15,
  }) async {
    await _channel.invokeMethod('updateTask', {
      'taskId': taskId,
      'title': title,
      'description': description,
      'bonusMinutes': bonusMinutes,
    });
  }

  /// Get task status for today (tasks with completion state, earned minutes, pending count).
  static Future<Map<String, dynamic>> getTaskStatus() async {
    final result = await _channel.invokeMethod('getTaskStatus');
    if (result == null) return {};
    return Map<String, dynamic>.from(result as Map);
  }

  /// Complete a task (requires parent PIN verification).
  static Future<Map<String, dynamic>> completeTask({
    required String taskId,
    required String pin,
  }) async {
    final result = await _channel.invokeMethod('completeTask', {
      'taskId': taskId,
      'pin': pin,
    });
    return Map<String, dynamic>.from(result as Map);
  }

  /// Set default bonus minutes per task.
  static Future<void> setBonusMinutesPerTask(int minutes) async {
    await _channel.invokeMethod('setBonusMinutesPerTask', {'minutes': minutes});
  }

  /// Get default bonus minutes per task.
  static Future<int> getBonusMinutesPerTask() async {
    return await _channel.invokeMethod('getBonusMinutesPerTask') ?? 15;
  }
}
