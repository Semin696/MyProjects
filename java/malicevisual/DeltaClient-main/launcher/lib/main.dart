import 'package:flutter/material.dart';
import 'services/config_service.dart';
import 'services/java_detector.dart';
import 'services/process_service.dart';
import 'views/about_view.dart';
import 'views/console_view.dart';
import 'views/dev_tools_view.dart';
import 'views/home_view.dart';
import 'views/settings_view.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await ConfigService.instance.loadConfig();
  runApp(const DeltaLauncherApp());
}

class DeltaLauncherApp extends StatelessWidget {
  const DeltaLauncherApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Delta Client Launcher',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0D1019),
        primaryColor: const Color(0xFF6366F1),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF6366F1),
          secondary: Color(0xFF10B981),
          surface: Color(0xFF161A29),
          error: Color(0xFFEF4444),
        ),
        fontFamily: 'Segoe UI',
      ),
      home: const MainShell(),
    );
  }
}

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _selectedTabIndex = 0;
  List<JavaInstallation> _installedJavas = [];
  bool _isLoadingJavas = true;

  @override
  void initState() {
    super.initState();
    _scanJavas();
    ProcessService.instance.addListener(_onProcessChange);
  }

  @override
  void dispose() {
    ProcessService.instance.removeListener(_onProcessChange);
    super.dispose();
  }

  void _onProcessChange() {
    setState(() {});
  }

  Future<void> _scanJavas() async {
    setState(() => _isLoadingJavas = true);
    final javas = await JavaDetector.detectInstalledJavas();
    setState(() {
      _installedJavas = javas;
      _isLoadingJavas = false;
    });

    final config = ConfigService.instance.config;
    if (config.selectedJavaPath.isEmpty && javas.isNotEmpty) {
      final preferred = javas.firstWhere(
        (j) => j.isCompatible,
        orElse: () => javas.first,
      );
      config.selectedJavaPath = preferred.path;
      ConfigService.instance.saveConfig();
    }
  }

  @override
  Widget build(BuildContext context) {
    final processState = ProcessService.instance.state;

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: RadialGradient(
            center: Alignment(-0.8, -0.9),
            radius: 1.5,
            colors: [
              Color(0xFF1A1D36),
              Color(0xFF0F121E),
              Color(0xFF090B12),
            ],
          ),
        ),
        child: Row(
          children: [
            // Left Navigation Sidebar
            _buildSidebar(processState),

            // Main Content Body
            Expanded(
              child: Column(
                children: [
                  // Top App Header
                  _buildTopBar(processState),

                  // Tab View Content
                  Expanded(
                    child: _buildCurrentView(),
                  ),

                  // Bottom Status Footer
                  _buildBottomBar(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSidebar(LauncherProcessState processState) {
    return Container(
      width: 240,
      decoration: const BoxDecoration(
        color: Color(0xFF111422),
        border: Border(
          right: BorderSide(color: Color(0xFF1E2438), width: 1.2),
        ),
      ),
      child: Column(
        children: [
          // Logo & Branding Header
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [Color(0xFF6366F1), Color(0xFF10B981)],
                    ),
                    borderRadius: BorderRadius.circular(10),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFF6366F1).withOpacity(0.4),
                        blurRadius: 12,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: const Center(
                    child: Text(
                      'Δ',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w900,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'DELTA CLIENT',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                        letterSpacing: 1.2,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      'Launcher v1.0',
                      style: TextStyle(
                        fontSize: 11,
                        color: Color(0xFF64748B),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const Divider(color: Color(0xFF1E2438), height: 1),
          const SizedBox(height: 16),

          // Nav Items
          _buildNavItem(0, Icons.rocket_launch_rounded, 'Главная'),
          _buildNavItem(
            1,
            Icons.terminal_rounded,
            'Консоль / Логи',
            badge: ProcessService.instance.logs.isNotEmpty
                ? '${ProcessService.instance.logs.length}'
                : null,
          ),
          _buildNavItem(2, Icons.handyman_rounded, 'Gradle Задачи'),
          _buildNavItem(3, Icons.settings_rounded, 'Настройки'),
          _buildNavItem(4, Icons.info_outline_rounded, 'О клиенте'),

          const Spacer(),

          // Quick Process Status in Sidebar
          _buildSidebarStatus(processState),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  Widget _buildNavItem(int index, IconData icon, String title, {String? badge}) {
    final isSelected = _selectedTabIndex == index;
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: isSelected ? const Color(0xFF6366F1).withOpacity(0.15) : Colors.transparent,
        borderRadius: BorderRadius.circular(12),
        border: isSelected
            ? Border.all(color: const Color(0xFF6366F1).withOpacity(0.5))
            : null,
      ),
      child: ListTile(
        onTap: () => setState(() => _selectedTabIndex = index),
        dense: true,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        leading: Icon(
          icon,
          color: isSelected ? const Color(0xFF818CF8) : const Color(0xFF94A3B8),
          size: 20,
        ),
        title: Text(
          title,
          style: TextStyle(
            fontSize: 13.5,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
            color: isSelected ? Colors.white : const Color(0xFF94A3B8),
          ),
        ),
        trailing: badge != null
            ? Container(
                padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFF6366F1).withOpacity(0.3),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  badge,
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFA5B4FC),
                  ),
                ),
              )
            : null,
      ),
    );
  }

  Widget _buildSidebarStatus(LauncherProcessState processState) {
    Color color;
    String text;

    switch (processState) {
      case LauncherProcessState.starting:
        color = const Color(0xFFF59E0B);
        text = 'Запуск...';
        break;
      case LauncherProcessState.running:
        color = const Color(0xFF10B981);
        text = 'Клиент активен';
        break;
      case LauncherProcessState.stopping:
        color = const Color(0xFFF43F5E);
        text = 'Остановка...';
        break;
      case LauncherProcessState.error:
        color = const Color(0xFFEF4444);
        text = 'Ошибка';
        break;
      case LauncherProcessState.success:
        color = const Color(0xFF10B981);
        text = 'Завершено';
        break;
      case LauncherProcessState.idle:
      default:
        color = const Color(0xFF64748B);
        text = 'Ожидание';
    }

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFF161A29),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFF1E2438)),
      ),
      child: Row(
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
              boxShadow: [
                BoxShadow(
                  color: color.withOpacity(0.6),
                  blurRadius: 6,
                ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: color,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTopBar(LauncherProcessState processState) {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 32),
      decoration: const BoxDecoration(
        color: Color(0xFF0F1322),
        border: Border(bottom: BorderSide(color: Color(0xFF1E2438))),
      ),
      child: Row(
        children: [
          Text(
            _getTabTitle(),
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          const Spacer(),
          if (_isLoadingJavas)
            const Row(
              children: [
                SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Color(0xFF818CF8),
                  ),
                ),
                SizedBox(width: 8),
                Text(
                  'Поиск Java...',
                  style: TextStyle(fontSize: 12, color: Color(0xFF64748B)),
                ),
              ],
            ),
        ],
      ),
    );
  }

  String _getTabTitle() {
    switch (_selectedTabIndex) {
      case 0:
        return 'Главная панель';
      case 1:
        return 'Консоль событий и логи';
      case 2:
        return 'Gradle Сборщик и Утилиты';
      case 3:
        return 'Конфигурация и Параметры';
      case 4:
        return 'Справка о Delta Client';
      default:
        return 'Delta Client';
    }
  }

  Widget _buildCurrentView() {
    switch (_selectedTabIndex) {
      case 0:
        return HomeView(
          installedJavas: _installedJavas,
          onOpenLogs: () => setState(() => _selectedTabIndex = 1),
          onRefreshJavas: _scanJavas,
        );
      case 1:
        return const ConsoleView();
      case 2:
        return DevToolsView(
          onOpenLogs: () => setState(() => _selectedTabIndex = 1),
        );
      case 3:
        return SettingsView(
          installedJavas: _installedJavas,
          onRefreshJavas: _scanJavas,
        );
      case 4:
        return const AboutView();
      default:
        return const Center(child: Text('Unknown View'));
    }
  }

  Widget _buildBottomBar() {
    final config = ConfigService.instance.config;
    final javaName = config.selectedJavaPath.isNotEmpty
        ? config.selectedJavaPath.split(RegExp(r'[\\/]')).last
        : 'Java не выбрана';

    return Container(
      height: 32,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      decoration: const BoxDecoration(
        color: Color(0xFF0B0E18),
        border: Border(top: BorderSide(color: Color(0xFF1E2438))),
      ),
      child: Row(
        children: [
          const Icon(Icons.code_rounded, size: 14, color: Color(0xFF64748B)),
          const SizedBox(width: 6),
          const Text(
            'Minecraft 1.21.4 • Fabric Loom',
            style: TextStyle(fontSize: 11, color: Color(0xFF64748B)),
          ),
          const Spacer(),
          const Icon(Icons.coffee_rounded, size: 14, color: Color(0xFF64748B)),
          const SizedBox(width: 6),
          Text(
            'JDK: $javaName',
            style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8)),
          ),
          const SizedBox(width: 16),
          const Icon(Icons.memory_rounded, size: 14, color: Color(0xFF64748B)),
          const SizedBox(width: 6),
          Text(
            'ОЗУ: ${config.ramGb}GB',
            style: const TextStyle(fontSize: 11, color: Color(0xFF94A3B8)),
          ),
        ],
      ),
    );
  }
}
