import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';

enum LauncherProcessState {
  idle,
  starting,
  running,
  stopping,
  success,
  error,
}

enum LogLevel {
  info,
  warning,
  error,
  gradle,
  fabric,
  system,
}

class LogEntry {
  final DateTime timestamp;
  final String text;
  final LogLevel level;

  LogEntry({
    required this.timestamp,
    required this.text,
    required this.level,
  });
}

class ProcessService extends ChangeNotifier {
  static final ProcessService instance = ProcessService._internal();
  ProcessService._internal();

  LauncherProcessState _state = LauncherProcessState.idle;
  LauncherProcessState get state => _state;

  Process? _activeProcess;
  int? get activePid => _activeProcess?.pid;

  String? _currentTask;
  String? get currentTask => _currentTask;

  DateTime? _startedAt;
  DateTime? get startedAt => _startedAt;

  final List<LogEntry> _logs = [];
  List<LogEntry> get logs => List.unmodifiable(_logs);

  Directory get projectRootDir {
    // Check if current directory has build.gradle
    final current = Directory.current;
    if (File('${current.path}\\build.gradle').existsSync()) {
      return current;
    }
    // Check parent directory
    final parent = current.parent;
    if (File('${parent.path}\\build.gradle').existsSync()) {
      return parent;
    }
    return current;
  }

  void _setState(LauncherProcessState newState) {
    _state = newState;
    notifyListeners();
  }

  void addLog(String text, [LogLevel level = LogLevel.info]) {
    final entry = LogEntry(
      timestamp: DateTime.now(),
      text: text,
      level: level,
    );
    _logs.add(entry);
    notifyListeners();
  }

  void clearLogs() {
    _logs.clear();
    notifyListeners();
  }

  LogLevel _classifyLog(String line) {
    final lower = line.toLowerCase();
    if (lower.contains('error') ||
        lower.contains('exception') ||
        lower.contains('failed') ||
        lower.contains('fatal') ||
        lower.contains('crash')) {
      return LogLevel.error;
    }
    if (lower.contains('warn') || lower.contains('warning')) {
      return LogLevel.warning;
    }
    if (line.startsWith('> Task') || lower.contains('build') || lower.contains('gradle')) {
      return LogLevel.gradle;
    }
    if (lower.contains('fabric') || lower.contains('net.fabricmc')) {
      return LogLevel.fabric;
    }
    return LogLevel.info;
  }

  Future<void> runTask({
    required String task,
    String? javaHome,
    int ramGb = 4,
    String customJvmArgs = '',
  }) async {
    if (_state == LauncherProcessState.running ||
        _state == LauncherProcessState.starting) {
      return;
    }

    _currentTask = task;
    _startedAt = DateTime.now();
    _setState(LauncherProcessState.starting);

    final rootDir = projectRootDir;
    final gradlewBat = File('${rootDir.path}\\gradlew.bat');

    if (!gradlewBat.existsSync()) {
      addLog(
        '[ERROR] gradlew.bat not found in ${rootDir.path}',
        LogLevel.error,
      );
      _setState(LauncherProcessState.error);
      return;
    }

    addLog(
      '====================================================',
      LogLevel.system,
    );
    addLog(
      '[LAUNCHER] Starting task: "$task" in ${rootDir.path}',
      LogLevel.system,
    );

    final env = Map<String, String>.from(Platform.environment);

    if (javaHome != null && javaHome.trim().isNotEmpty) {
      env['JAVA_HOME'] = javaHome.trim();
      final pathVar = env['PATH'] ?? '';
      env['PATH'] = '${javaHome.trim()}\\bin;$pathVar';
      addLog('[LAUNCHER] Using JAVA_HOME: $javaHome', LogLevel.system);
    }

    final jvmArgs = '-Xmx${ramGb}G $customJvmArgs'.trim();
    env['GRADLE_OPTS'] = jvmArgs;
    addLog('[LAUNCHER] RAM Allocated: ${ramGb}GB | JVM Opts: $jvmArgs', LogLevel.system);
    addLog(
      '====================================================',
      LogLevel.system,
    );

    try {
      final process = await Process.start(
        'cmd.exe',
        ['/c', 'gradlew.bat', task],
        workingDirectory: rootDir.path,
        environment: env,
      );

      _activeProcess = process;
      _setState(LauncherProcessState.running);
      addLog('[LAUNCHER] Process spawned (PID: ${process.pid})', LogLevel.system);

      process.stdout
          .transform(utf8.decoder)
          .transform(const LineSplitter())
          .listen((line) {
        if (line.trim().isNotEmpty) {
          addLog(line, _classifyLog(line));
        }
      });

      process.stderr
          .transform(utf8.decoder)
          .transform(const LineSplitter())
          .listen((line) {
        if (line.trim().isNotEmpty) {
          addLog(line, LogLevel.error);
        }
      });

      final exitCode = await process.exitCode;
      _activeProcess = null;

      if (exitCode == 0) {
        addLog(
          '[LAUNCHER] Task "$task" completed successfully! (exit code 0)',
          LogLevel.system,
        );
        _setState(LauncherProcessState.success);
      } else {
        addLog(
          '[LAUNCHER] Task "$task" exited with error code: $exitCode',
          LogLevel.error,
        );
        _setState(LauncherProcessState.error);
      }
    } catch (e) {
      addLog('[LAUNCHER] Exception launching task: $e', LogLevel.error);
      _setState(LauncherProcessState.error);
    }
  }

  Future<void> killCurrentProcess() async {
    if (_activeProcess == null) return;
    _setState(LauncherProcessState.stopping);
    addLog('[LAUNCHER] Terminating process...', LogLevel.warning);

    final pid = _activeProcess!.pid;
    try {
      // Force kill entire process tree on Windows
      await Process.run('taskkill', ['/F', '/T', '/PID', '$pid']);
      _activeProcess?.kill(ProcessSignal.sigkill);
      addLog('[LAUNCHER] Process (PID: $pid) stopped.', LogLevel.system);
    } catch (e) {
      addLog('[LAUNCHER] Error killing process: $e', LogLevel.error);
    } finally {
      _activeProcess = null;
      _setState(LauncherProcessState.idle);
    }
  }
}
