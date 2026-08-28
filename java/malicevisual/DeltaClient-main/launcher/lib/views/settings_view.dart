import 'dart:io';
import 'package:flutter/material.dart';
import '../services/config_service.dart';
import '../services/java_detector.dart';
import '../services/process_service.dart';
import '../widgets/glass_card.dart';

class SettingsView extends StatefulWidget {
  final List<JavaInstallation> installedJavas;
  final VoidCallback onRefreshJavas;

  const SettingsView({
    super.key,
    required this.installedJavas,
    required this.onRefreshJavas,
  });

  @override
  State<SettingsView> createState() => _SettingsViewState();
}

class _SettingsViewState extends State<SettingsView> {
  final _configService = ConfigService.instance;
  late final TextEditingController _customJavaController;
  late final TextEditingController _jvmArgsController;

  @override
  void initState() {
    super.initState();
    _customJavaController = TextEditingController(
      text: _configService.config.selectedJavaPath,
    );
    _jvmArgsController = TextEditingController(
      text: _configService.config.customJvmArgs,
    );
  }

  @override
  void dispose() {
    _customJavaController.dispose();
    _jvmArgsController.dispose();
    super.dispose();
  }

  void _resetToDefaults() {
    setState(() {
      _configService.config.ramGb = 4;
      _configService.config.customJvmArgs =
          '-XX:+UseG1GC -XX:+ParallelRefProcEnabled';
      _jvmArgsController.text = _configService.config.customJvmArgs;
      if (widget.installedJavas.isNotEmpty) {
        _configService.config.selectedJavaPath =
            widget.installedJavas.first.path;
        _customJavaController.text = _configService.config.selectedJavaPath;
      }
    });
    _configService.saveConfig();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Настройки сброшены по умолчанию'),
        backgroundColor: Color(0xFF6366F1),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final config = _configService.config;
    final rootDir = ProcessService.instance.projectRootDir.path;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Настройки лаунчера',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Управление средой Java, аргументами виртуальной машины и памятью',
            style: TextStyle(fontSize: 14, color: Color(0xFF94A3B8)),
          ),
          const SizedBox(height: 24),

          // Java Configuration Card
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Row(
                      children: [
                        Icon(Icons.coffee_rounded, color: Color(0xFFF59E0B), size: 22),
                        SizedBox(width: 10),
                        Text(
                          'Конфигурация Java / JDK',
                          style: TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                    ElevatedButton.icon(
                      onPressed: () {
                        widget.onRefreshJavas();
                        setState(() {
                          _customJavaController.text = config.selectedJavaPath;
                        });
                      },
                      icon: const Icon(Icons.sync_rounded, size: 16),
                      label: const Text('Пересканировать'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF1E293B),
                        foregroundColor: const Color(0xFF94A3B8),
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                const Text(
                  'Путь к Java Home (папка JDK):',
                  style: TextStyle(fontSize: 13, color: Color(0xFFCBD5E1)),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _customJavaController,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 13,
                    fontFamily: 'Consolas, monospace',
                  ),
                  decoration: InputDecoration(
                    hintText: 'C:\\Program Files\\Java\\jdk-26.0.1',
                    hintStyle: const TextStyle(color: Color(0xFF64748B)),
                    prefixIcon: const Icon(Icons.folder_rounded, color: Color(0xFFF59E0B)),
                    filled: true,
                    fillColor: const Color(0xFF0F172A),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: Color(0xFF334155)),
                    ),
                  ),
                  onChanged: (val) {
                    config.selectedJavaPath = val.trim();
                    _configService.saveConfig();
                  },
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: widget.installedJavas.map((j) {
                    final isSelected = config.selectedJavaPath == j.path;
                    return ActionChip(
                      backgroundColor: isSelected
                          ? const Color(0xFF6366F1).withOpacity(0.3)
                          : const Color(0xFF0F172A),
                      side: BorderSide(
                        color: isSelected
                            ? const Color(0xFF6366F1)
                            : const Color(0xFF334155),
                      ),
                      label: Text(
                        '${j.name} (${j.version})',
                        style: TextStyle(
                          fontSize: 12,
                          color: isSelected ? Colors.white : const Color(0xFF94A3B8),
                          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                        ),
                      ),
                      onPressed: () {
                        setState(() {
                          config.selectedJavaPath = j.path;
                          _customJavaController.text = j.path;
                        });
                        _configService.saveConfig();
                      },
                    );
                  }).toList(),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // JVM Arguments Card
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.memory_rounded, color: Color(0xFF6366F1), size: 22),
                    SizedBox(width: 10),
                    Text(
                      'Параметры JVM и Garbage Collector',
                      style: TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                const Text(
                  'Дополнительные флаги JVM:',
                  style: TextStyle(fontSize: 13, color: Color(0xFFCBD5E1)),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _jvmArgsController,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 13,
                    fontFamily: 'Consolas, monospace',
                  ),
                  decoration: InputDecoration(
                    hintText: '-XX:+UseG1GC -XX:+ParallelRefProcEnabled',
                    hintStyle: const TextStyle(color: Color(0xFF64748B)),
                    prefixIcon: const Icon(Icons.settings_ethernet_rounded, color: Color(0xFF6366F1)),
                    filled: true,
                    fillColor: const Color(0xFF0F172A),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: Color(0xFF334155)),
                    ),
                  ),
                  onChanged: (val) {
                    config.customJvmArgs = val.trim();
                    _configService.saveConfig();
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Environment & Paths
          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.location_on_outlined, color: Color(0xFF10B981), size: 22),
                    SizedBox(width: 10),
                    Text(
                      'Рабочие директории',
                      style: TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                _buildPathRow('Корень проекта:', rootDir),
                const Divider(color: Color(0xFF334155), height: 20),
                _buildPathRow(
                  'Конфиг лаунчера:',
                  '${rootDir}\\launcher_config.json',
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // Reset button
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              OutlinedButton.icon(
                onPressed: _resetToDefaults,
                icon: const Icon(Icons.restore_rounded, size: 18),
                label: const Text('Сбросить по умолчанию'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFFEF4444),
                  side: const BorderSide(color: Color(0xFFEF4444)),
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPathRow(String title, String path) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 160,
          child: Text(
            title,
            style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 13),
          ),
        ),
        Expanded(
          child: SelectableText(
            path,
            style: const TextStyle(
              color: Color(0xFFE2E8F0),
              fontSize: 12.5,
              fontFamily: 'Consolas, monospace',
            ),
          ),
        ),
      ],
    );
  }
}
