import 'dart:io';
import 'package:flutter/material.dart';
import '../services/config_service.dart';
import '../services/process_service.dart';
import '../widgets/glass_card.dart';

class DevToolsView extends StatelessWidget {
  final VoidCallback onOpenLogs;

  const DevToolsView({super.key, required this.onOpenLogs});

  void _runTask(String task) {
    final config = ConfigService.instance.config;
    ProcessService.instance.runTask(
      task: task,
      javaHome: config.selectedJavaPath,
      ramGb: config.ramGb,
      customJvmArgs: config.customJvmArgs,
    );
    onOpenLogs();
  }

  void _openBuildLibsFolder() {
    final rootDir = ProcessService.instance.projectRootDir;
    final libsDir = Directory('${rootDir.path}\\build\\libs');
    if (!libsDir.existsSync()) {
      libsDir.createSync(recursive: true);
    }
    Process.run('explorer.exe', [libsDir.path]);
  }

  @override
  Widget build(BuildContext context) {
    final processState = ProcessService.instance.state;
    final isRunning = processState == LauncherProcessState.running ||
        processState == LauncherProcessState.starting;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Инструменты разработчика (Gradle Tools)',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Быстрый запуск сборки, генерации исходников Loom и управления проектом',
            style: TextStyle(fontSize: 14, color: Color(0xFF94A3B8)),
          ),
          const SizedBox(height: 24),

          // Action Grid
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisSpacing: 18,
            mainAxisSpacing: 18,
            childAspectRatio: 2.2,
            children: [
              _buildTaskCard(
                title: 'runClient',
                description: 'Запуск Minecraft 1.21.4 в среде разработки Fabric',
                icon: Icons.play_circle_outline_rounded,
                color: const Color(0xFF6366F1),
                isRunning: isRunning,
                onTap: () => _runTask('runClient'),
              ),
              _buildTaskCard(
                title: 'build',
                description: 'Компиляция и упаковка мода в релизный JAR-архив',
                icon: Icons.inventory_2_outlined,
                color: const Color(0xFF10B981),
                isRunning: isRunning,
                onTap: () => _runTask('build'),
              ),
              _buildTaskCard(
                title: 'clean runClient',
                description: 'Полная очистка кэша и перезапуск клиента',
                icon: Icons.replay_rounded,
                color: const Color(0xFF0EA5E9),
                isRunning: isRunning,
                onTap: () => _runTask('clean runClient'),
              ),
              _buildTaskCard(
                title: 'genSources',
                description: 'Генерация исходников Minecraft и маппингов Yarn',
                icon: Icons.code_rounded,
                color: const Color(0xFF8B5CF6),
                isRunning: isRunning,
                onTap: () => _runTask('genSources'),
              ),
              _buildTaskCard(
                title: 'clean',
                description: 'Удаление директории build и временных артефактов',
                icon: Icons.delete_outline_rounded,
                color: const Color(0xFFF59E0B),
                isRunning: isRunning,
                onTap: () => _runTask('clean'),
              ),
              _buildTaskCard(
                title: 'Открыть build/libs',
                description: 'Открыть папку с готовыми скомпилированными JAR',
                icon: Icons.folder_zip_outlined,
                color: const Color(0xFFEC4899),
                isRunning: false,
                onTap: _openBuildLibsFolder,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTaskCard({
    required String title,
    required String description,
    required IconData icon,
    required Color color,
    required bool isRunning,
    required VoidCallback onTap,
  }) {
    return GlassCard(
      onTap: isRunning ? null : onTap,
      padding: const EdgeInsets.all(18),
      borderColor: color.withOpacity(0.3),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: color.withOpacity(0.15),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: color.withOpacity(0.4)),
            ),
            child: Icon(icon, color: color, size: 26),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: color,
                    fontFamily: 'Consolas, monospace',
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: const TextStyle(
                    fontSize: 12,
                    color: Color(0xFF94A3B8),
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          const Icon(
            Icons.chevron_right_rounded,
            color: Color(0xFF475569),
            size: 20,
          ),
        ],
      ),
    );
  }
}
