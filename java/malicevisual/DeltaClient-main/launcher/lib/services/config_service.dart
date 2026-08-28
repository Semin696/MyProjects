import 'dart:convert';
import 'dart:io';

class LauncherConfig {
  String selectedJavaPath;
  int ramGb;
  String username;
  String customJvmArgs;
  bool autoScrollLogs;
  String lastGradleTask;

  LauncherConfig({
    this.selectedJavaPath = '',
    this.ramGb = 4,
    this.username = 'DeltaUser',
    this.customJvmArgs = '-XX:+UseG1GC -XX:+ParallelRefProcEnabled',
    this.autoScrollLogs = true,
    this.lastGradleTask = 'runClient',
  });

  Map<String, dynamic> toJson() => {
        'selectedJavaPath': selectedJavaPath,
        'ramGb': ramGb,
        'username': username,
        'customJvmArgs': customJvmArgs,
        'autoScrollLogs': autoScrollLogs,
        'lastGradleTask': lastGradleTask,
      };

  factory LauncherConfig.fromJson(Map<String, dynamic> json) => LauncherConfig(
        selectedJavaPath: json['selectedJavaPath'] as String? ?? '',
        ramGb: (json['ramGb'] as num?)?.toInt() ?? 4,
        username: json['username'] as String? ?? 'DeltaUser',
        customJvmArgs: json['customJvmArgs'] as String? ??
            '-XX:+UseG1GC -XX:+ParallelRefProcEnabled',
        autoScrollLogs: json['autoScrollLogs'] as bool? ?? true,
        lastGradleTask: json['lastGradleTask'] as String? ?? 'runClient',
      );
}

class ConfigService {
  static final ConfigService instance = ConfigService._internal();
  ConfigService._internal();

  LauncherConfig _config = LauncherConfig();
  LauncherConfig get config => _config;

  File get _configFile {
    // Save in parent root or launcher directory
    final parentDir = Directory.current.parent;
    final inParent = File('${parentDir.path}${Platform.pathSeparator}launcher_config.json');
    if (inParent.existsSync()) return inParent;
    return File('${Directory.current.path}${Platform.pathSeparator}launcher_config.json');
  }

  Future<void> loadConfig() async {
    try {
      final file = _configFile;
      if (await file.exists()) {
        final content = await file.readAsString();
        final json = jsonDecode(content) as Map<String, dynamic>;
        _config = LauncherConfig.fromJson(json);
      }
    } catch (e) {
      // Use defaults if corrupt or missing
    }
  }

  Future<void> saveConfig() async {
    try {
      final file = _configFile;
      await file.writeAsString(
        const JsonEncoder.withIndent('  ').convert(_config.toJson()),
      );
    } catch (e) {
      // Ignore save error
    }
  }
}
