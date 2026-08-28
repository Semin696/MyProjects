import 'dart:io';
import 'package:flutter/material.dart';
import '../services/process_service.dart';
import '../widgets/glass_card.dart';

class AboutView extends StatelessWidget {
  const AboutView({super.key});

  void _openFolder(String path) {
    try {
      Process.run('explorer.exe', [path]);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final rootDir = ProcessService.instance.projectRootDir.path;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'О проекте Delta Client',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Fabric Client для Minecraft 1.21.4 • Полная готовность для IntelliJ IDEA',
            style: TextStyle(fontSize: 14, color: Color(0xFF94A3B8)),
          ),
          const SizedBox(height: 24),

          GlassCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.diamond_rounded, color: Color(0xFF10B981), size: 24),
                    SizedBox(width: 10),
                    Text(
                      'Delta Client 1.21.4',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                const Text(
                  'Полностью очищенный Delta Client от мусора, ненужного кода, лишних импортов и аннотаций. '
                  'Скомпонован под актуальные версии Fabric API, Yarn Mappings и GeckoLib 4.',
                  style: TextStyle(fontSize: 14, color: Color(0xFFCBD5E1), height: 1.5),
                ),
                const SizedBox(height: 20),
                const Text(
                  'Встроенные модули:',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF818CF8),
                  ),
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _buildFeatureChip('AutoBuy (AH & Collector)'),
                    _buildFeatureChip('Ambience & Visuals'),
                    _buildFeatureChip('BlockESP & PlayerESP'),
                    _buildFeatureChip('Custom Macro Engine'),
                    _buildFeatureChip('Friend System'),
                    _buildFeatureChip('GPS & Waypoints'),
                    _buildFeatureChip('GeckoLib 3D Models'),
                    _buildFeatureChip('Command System (.vclip, .hclip, .rct)'),
                  ],
                ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    ElevatedButton.icon(
                      onPressed: () => _openFolder(rootDir),
                      icon: const Icon(Icons.folder_open_rounded, size: 18),
                      label: const Text('Открыть проект в проводнике'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF6366F1),
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFeatureChip(String title) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFF334155)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.check_rounded, color: Color(0xFF10B981), size: 16),
          const SizedBox(width: 6),
          Text(
            title,
            style: const TextStyle(fontSize: 12.5, color: Color(0xFFE2E8F0)),
          ),
        ],
      ),
    );
  }
}
