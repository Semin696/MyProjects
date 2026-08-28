import 'dart:io';
import 'package:flutter/material.dart';
import '../services/config_service.dart';
import '../services/java_detector.dart';
import '../services/process_service.dart';
import '../widgets/glass_card.dart';

class HomeView extends StatefulWidget {
  final List<JavaInstallation> installedJavas;
  final VoidCallback onOpenLogs;
  final VoidCallback onRefreshJavas;

  const HomeView({
    super.key,
    required this.installedJavas,
    required this.onOpenLogs,
    required this.onRefreshJavas,
  });

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends State<HomeView> {
  final _configService = ConfigService.instance;
  final _processService = ProcessService.instance;
  late final TextEditingController _usernameController;

  @override
  void initState() {
    super.initState();
    _usernameController = TextEditingController(
      text: _configService.config.username,
    );
  }

  @override
  void dispose() {
    _usernameController.dispose();
    super.dispose();
  }

  void _openFolder(String path) {
    try {
      Process.run('explorer.exe', [path]);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final config = _configService.config;
    final processState = _processService.state;
    final isRunning = processState == LauncherProcessState.running ||
        processState == LauncherProcessState.starting;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Banner / Header Card
          _buildHeroBanner(isRunning),
          const SizedBox(height: 24),

          // Main Controls & Info Row
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Left Column: Quick Config (RAM, Username, Java)
              Expanded(
                flex: 5,
                child: Column(
                  children: [
                    _buildSettingsCard(config, isRunning),
                    const SizedBox(height: 20),
                    _buildQuickActionsCard(),
                  ],
                ),
              ),
              const SizedBox(width: 24),

              // Right Column: Client Specs & Status
              Expanded(
                flex: 4,
                child: Column(
                  children: [
                    _buildStatusCard(processState),
                    const SizedBox(height: 20),
                    _buildClientInfoCard(),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildHeroBanner(bool isRunning) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: const LinearGradient(
          colors: [
            Color(0xFF1E1B4B),
            Color(0xFF0F172A),
            Color(0xFF022C22),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        border: Border.all(
          color: const Color(0xFF6366F1).withOpacity(0.4),
          width: 1.5,
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF6366F1).withOpacity(0.2),
            blurRadius: 30,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(28.0),
        child: Row(
          children: [
            // Glowing Delta Icon
            Container(
              width: 76,
              height: 76,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: const LinearGradient(
                  colors: [Color(0xFF818CF8), Color(0xFF10B981)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF818CF8).withOpacity(0.5),
                    blurRadius: 20,
                    spreadRadius: 2,
                  ),
                ],
              ),
              child: const Center(
                child: Text(
                  'Δ',
                  style: TextStyle(
                    fontSize: 44,
                    fontWeight: FontWeight.w900,
                    color: Colors.white,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 24),

            // Title & Info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Text(
                        'DELTA CLIENT',
                        style: TextStyle(
                          fontSize: 28,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 2.0,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0xFF10B981).withOpacity(0.2),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                            color: const Color(0xFF10B981),
                            width: 1,
                          ),
                        ),
                        child: const Text(
                          'v1.0.0 • 1.21.4',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF34D399),
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    'Fabric Client • Оптимизированный билд без мусора',
                    style: TextStyle(
                      fontSize: 14,
                      color: Color(0xFF94A3B8),
                    ),
                  ),
                ],
              ),
            ),

            // Big Launch / Stop Button
            _buildLaunchButton(isRunning),
          ],
        ),
      ),
    );
  }

  Widget _buildLaunchButton(bool isRunning) {
    if (isRunning) {
      return ElevatedButton.icon(
        onPressed: () => _processService.killCurrentProcess(),
        icon: const Icon(Icons.stop_rounded, size: 28, color: Colors.white),
        label: const Text(
          'ОСТАНОВИТЬ',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w800,
            letterSpacing: 1.2,
            color: Colors.white,
          ),
        ),
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color(0xFFE11D48),
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 22),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          elevation: 12,
          shadowColor: const Color(0xFFE11D48).withOpacity(0.6),
        ),
      );
    }

    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: const LinearGradient(
          colors: [Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFF059669)],
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF6366F1).withOpacity(0.5),
            blurRadius: 20,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: ElevatedButton.icon(
        onPressed: () {
          _processService.runTask(
            task: 'runClient',
            javaHome: _configService.config.selectedJavaPath,
            ramGb: _configService.config.ramGb,
            customJvmArgs: _configService.config.customJvmArgs,
          );
        },
        icon: const Icon(Icons.play_arrow_rounded, size: 32, color: Colors.white),
        label: const Text(
          'ЗАПУСК КЛИЕНТА',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w900,
            letterSpacing: 1.5,
            color: Colors.white,
          ),
        ),
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.transparent,
          shadowColor: Colors.transparent,
          padding: const EdgeInsets.symmetric(horizontal: 36, vertical: 22),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
      ),
    );
  }

  Widget _buildSettingsCard(LauncherConfig config, bool isRunning) {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.tune_rounded, color: Color(0xFF818CF8), size: 20),
              SizedBox(width: 10),
              Text(
                'Быстрые параметры',
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),

          // RAM Slider
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Выделение ОЗУ (RAM):',
                style: TextStyle(fontSize: 14, color: Color(0xFFCBD5E1)),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFF6366F1).withOpacity(0.2),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: const Color(0xFF6366F1), width: 1),
                ),
                child: Text(
                  '${config.ramGb} GB',
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFA5B4FC),
                  ),
                ),
              ),
            ],
          ),
          SliderTheme(
            data: SliderTheme.of(context).copyWith(
              activeTrackColor: const Color(0xFF6366F1),
              inactiveTrackColor: const Color(0xFF334155),
              thumbColor: const Color(0xFF818CF8),
              overlayColor: const Color(0xFF6366F1).withOpacity(0.2),
              trackHeight: 6,
            ),
            child: Slider(
              value: config.ramGb.toDouble(),
              min: 2,
              max: 16,
              divisions: 14,
              onChanged: isRunning
                  ? null
                  : (val) {
                      setState(() {
                        config.ramGb = val.toInt();
                      });
                      _configService.saveConfig();
                    },
            ),
          ),
          const SizedBox(height: 16),

          // Username Field
          const Text(
            'Никнейм игрока:',
            style: TextStyle(fontSize: 14, color: Color(0xFFCBD5E1)),
          ),
          const SizedBox(height: 8),
          TextField(
            controller: _usernameController,
            enabled: !isRunning,
            style: const TextStyle(color: Colors.white, fontSize: 14),
            decoration: InputDecoration(
              hintText: 'DeltaUser',
              hintStyle: const TextStyle(color: Color(0xFF64748B)),
              prefixIcon: const Icon(Icons.person_rounded, color: Color(0xFF818CF8)),
              filled: true,
              fillColor: const Color(0xFF0F172A),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFF334155)),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFF334155)),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFF6366F1)),
              ),
            ),
            onChanged: (val) {
              config.username = val.trim();
              _configService.saveConfig();
            },
          ),
          const SizedBox(height: 16),

          // Java Version Selector
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Версия Java (JDK):',
                style: TextStyle(fontSize: 14, color: Color(0xFFCBD5E1)),
              ),
              IconButton(
                onPressed: widget.onRefreshJavas,
                icon: const Icon(Icons.refresh_rounded, size: 18, color: Color(0xFF94A3B8)),
                tooltip: 'Обновить список Java',
              ),
            ],
          ),
          const SizedBox(height: 6),
          _buildJavaDropdown(config, isRunning),
        ],
      ),
    );
  }

  Widget _buildJavaDropdown(LauncherConfig config, bool isRunning) {
    if (widget.installedJavas.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: const Color(0xFFE11D48).withOpacity(0.1),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: const Color(0xFFE11D48)),
        ),
        child: const Row(
          children: [
            Icon(Icons.warning_amber_rounded, color: Color(0xFFF43F5E), size: 20),
            SizedBox(width: 10),
            Expanded(
              child: Text(
                'Java не обнаружена. Требуется Java 21+',
                style: TextStyle(color: Color(0xFFFDA4AF), fontSize: 13),
              ),
            ),
          ],
        ),
      );
    }

    final validSelection = widget.installedJavas.any(
      (j) => j.path == config.selectedJavaPath,
    );

    if (!validSelection && widget.installedJavas.isNotEmpty) {
      // Pick first compatible one
      final preferred = widget.installedJavas.firstWhere(
        (j) => j.isCompatible,
        orElse: () => widget.installedJavas.first,
      );
      config.selectedJavaPath = preferred.path;
      _configService.saveConfig();
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFF0F172A),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF334155)),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: config.selectedJavaPath,
          isExpanded: true,
          dropdownColor: const Color(0xFF1E293B),
          icon: const Icon(Icons.keyboard_arrow_down_rounded, color: Color(0xFF818CF8)),
          items: widget.installedJavas.map((j) {
            return DropdownMenuItem<String>(
              value: j.path,
              child: Row(
                children: [
                  Icon(
                    j.isCompatible ? Icons.check_circle_rounded : Icons.info_outline_rounded,
                    color: j.isCompatible ? const Color(0xFF10B981) : const Color(0xFFF59E0B),
                    size: 16,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      '${j.name} (v${j.version})',
                      style: TextStyle(
                        color: j.isCompatible ? Colors.white : const Color(0xFF94A3B8),
                        fontSize: 13,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
          onChanged: isRunning
              ? null
              : (val) {
                  if (val != null) {
                    setState(() {
                      config.selectedJavaPath = val;
                    });
                    _configService.saveConfig();
                  }
                },
        ),
      ),
    );
  }

  Widget _buildQuickActionsCard() {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.flash_on_rounded, color: Color(0xFF10B981), size: 20),
              SizedBox(width: 10),
              Text(
                'Быстрые действия',
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _buildActionChip(
                  icon: Icons.build_rounded,
                  label: 'Собрать JAR',
                  color: const Color(0xFF6366F1),
                  onTap: () {
                    _processService.runTask(
                      task: 'build',
                      javaHome: _configService.config.selectedJavaPath,
                      ramGb: _configService.config.ramGb,
                    );
                    widget.onOpenLogs();
                  },
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildActionChip(
                  icon: Icons.cleaning_services_rounded,
                  label: 'Очистить кэш',
                  color: const Color(0xFF0EA5E9),
                  onTap: () {
                    _processService.runTask(
                      task: 'clean',
                      javaHome: _configService.config.selectedJavaPath,
                    );
                    widget.onOpenLogs();
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildActionChip(
                  icon: Icons.folder_open_rounded,
                  label: 'Папка проекта',
                  color: const Color(0xFF8B5CF6),
                  onTap: () => _openFolder(_processService.projectRootDir.path),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildActionChip(
                  icon: Icons.terminal_rounded,
                  label: 'Консоль логов',
                  color: const Color(0xFF10B981),
                  onTap: widget.onOpenLogs,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildActionChip({
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onTap,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.35)),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 12),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(icon, color: color, size: 18),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(
                    label,
                    style: TextStyle(
                      color: color,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStatusCard(LauncherProcessState state) {
    Color statusColor;
    String statusTitle;
    String statusSubtitle;
    IconData statusIcon;

    switch (state) {
      case LauncherProcessState.starting:
        statusColor = const Color(0xFFF59E0B);
        statusTitle = 'Инициализация...';
        statusSubtitle = 'Подготовка окружения и Gradle';
        statusIcon = Icons.hourglass_top_rounded;
        break;
      case LauncherProcessState.running:
        statusColor = const Color(0xFF10B981);
        statusTitle = 'Клиент запущен';
        statusSubtitle = 'PID: ${_processService.activePid ?? '-'} • Игра активна';
        statusIcon = Icons.sports_esports_rounded;
        break;
      case LauncherProcessState.stopping:
        statusColor = const Color(0xFFF43F5E);
        statusTitle = 'Остановка...';
        statusSubtitle = 'Завершение процесса';
        statusIcon = Icons.stop_circle_outlined;
        break;
      case LauncherProcessState.success:
        statusColor = const Color(0xFF10B981);
        statusTitle = 'Успешно завершено';
        statusSubtitle = 'Код возврата 0';
        statusIcon = Icons.check_circle_rounded;
        break;
      case LauncherProcessState.error:
        statusColor = const Color(0xFFEF4444);
        statusTitle = 'Ошибка выполнения';
        statusSubtitle = 'Проверьте логи в консоли';
        statusIcon = Icons.error_outline_rounded;
        break;
      case LauncherProcessState.idle:
      default:
        statusColor = const Color(0xFF94A3B8);
        statusTitle = 'Готов к запуску';
        statusSubtitle = 'Ожидание команды пользователя';
        statusIcon = Icons.radio_button_checked_rounded;
    }

    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(statusIcon, color: statusColor, size: 22),
              const SizedBox(width: 10),
              Text(
                statusTitle,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: statusColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            statusSubtitle,
            style: const TextStyle(fontSize: 13, color: Color(0xFF94A3B8)),
          ),
        ],
      ),
    );
  }

  Widget _buildClientInfoCard() {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.info_outline_rounded, color: Color(0xFF0EA5E9), size: 20),
              SizedBox(width: 10),
              Text(
                'Информация о сборке',
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _buildInfoRow('Версия Minecraft', '1.21.4'),
          const Divider(color: Color(0xFF334155), height: 20),
          _buildInfoRow('Модлоадер', 'Fabric Loader 0.18.4'),
          const Divider(color: Color(0xFF334155), height: 20),
          _buildInfoRow('Fabric API', '0.119.4+1.21.4'),
          const Divider(color: Color(0xFF334155), height: 20),
          _buildInfoRow('GeckoLib', '4.8.5'),
          const Divider(color: Color(0xFF334155), height: 20),
          _buildInfoRow('Среда разработки', 'IntelliJ / Gradle 9.7'),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 13, color: Color(0xFF94A3B8)),
        ),
        Text(
          value,
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: Color(0xFFE2E8F0),
          ),
        ),
      ],
    );
  }
}
