import 'dart:io';

class JavaInstallation {
  final String name;
  final String path;
  final String version;
  final int majorVersion;
  final bool isCompatible; // Java 21+

  JavaInstallation({
    required this.name,
    required this.path,
    required this.version,
    required this.majorVersion,
    required this.isCompatible,
  });

  @override
  String toString() => '$name (Java $version)';
}

class JavaDetector {
  static Future<List<JavaInstallation>> detectInstalledJavas() async {
    final Map<String, JavaInstallation> discovered = {};
    final userProfile = Platform.environment['USERPROFILE'] ?? '';

    final searchRoots = <String>[
      if (Platform.environment['JAVA_HOME'] != null)
        Platform.environment['JAVA_HOME']!,
      'C:\\Program Files\\Java',
      'C:\\Program Files (x86)\\Java',
      if (userProfile.isNotEmpty) '$userProfile\\.jdks',
      if (userProfile.isNotEmpty) '$userProfile\\.gradle\\jdks',
      'C:\\Program Files\\Eclipse Adoptium',
      'C:\\Program Files\\Microsoft',
      'C:\\Program Files\\BellSoft',
      'C:\\Program Files\\Amazon Corretto',
    ];

    for (final rootPath in searchRoots) {
      final dir = Directory(rootPath);
      if (!dir.existsSync()) continue;

      // Check if rootPath itself contains bin/java.exe
      final directJava = File('$rootPath\\bin\\java.exe');
      if (directJava.existsSync()) {
        await _inspectAndAdd(directJava.path, discovered);
      }

      // Check subdirectories (e.g., jdk-26.0.1, ms-25.0.3)
      try {
        final entities = dir.listSync();
        for (final entity in entities) {
          if (entity is Directory) {
            final subJava = File('${entity.path}\\bin\\java.exe');
            if (subJava.existsSync()) {
              await _inspectAndAdd(subJava.path, discovered);
            }
          }
        }
      } catch (_) {}
    }

    // Check system PATH
    try {
      final result = await Process.run('where', ['java.exe']);
      if (result.exitCode == 0) {
        final lines = (result.stdout as String).split(RegExp(r'\r?\n'));
        for (final line in lines) {
          final trimmed = line.trim();
          if (trimmed.isNotEmpty && File(trimmed).existsSync()) {
            await _inspectAndAdd(trimmed, discovered);
          }
        }
      }
    } catch (_) {}

    final list = discovered.values.toList();
    // Sort descending by major version
    list.sort((a, b) => b.majorVersion.compareTo(a.majorVersion));
    return list;
  }

  static Future<void> _inspectAndAdd(
    String javaExePath,
    Map<String, JavaInstallation> map,
  ) async {
    final normalized = javaExePath.toLowerCase();
    if (map.containsKey(normalized)) return;

    try {
      final result = await Process.run(javaExePath, ['-version']);
      final output = '${result.stdout}\n${result.stderr}';
      
      // Parse version e.g. "21.0.2" or "26.0.1" or "1.8.0_491"
      final versionMatch = RegExp(r'version "([^"]+)"').firstMatch(output);
      final versionStr = versionMatch?.group(1) ?? 'Unknown';
      
      int major = 0;
      if (versionStr.startsWith('1.')) {
        // e.g. 1.8.0
        final parts = versionStr.split('.');
        if (parts.length > 1) major = int.tryParse(parts[1]) ?? 8;
      } else {
        final parts = versionStr.split('.');
        if (parts.isNotEmpty) major = int.tryParse(parts[0]) ?? 0;
      }

      final parentFolder = Directory(File(javaExePath).parent.parent.path).path;
      final folderName = parentFolder.split(Platform.pathSeparator).last;

      final installation = JavaInstallation(
        name: folderName.isNotEmpty ? folderName : 'Java $major',
        path: parentFolder,
        version: versionStr,
        majorVersion: major,
        isCompatible: major >= 21,
      );

      map[normalized] = installation;
    } catch (_) {}
  }
}
