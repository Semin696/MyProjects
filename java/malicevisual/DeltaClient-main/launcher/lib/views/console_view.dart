import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/process_service.dart';

class ConsoleView extends StatefulWidget {
  const ConsoleView({super.key});

  @override
  State<ConsoleView> createState() => _ConsoleViewState();
}

class _ConsoleViewState extends State<ConsoleView> {
  final _processService = ProcessService.instance;
  final _scrollController = ScrollController();
  final _filterController = TextEditingController();
  bool _autoScroll = true;
  LogLevel? _selectedLevel;

  @override
  void initState() {
    super.initState();
    _processService.addListener(_onNewLog);
  }

  @override
  void dispose() {
    _processService.removeListener(_onNewLog);
    _scrollController.dispose();
    _filterController.dispose();
    super.dispose();
  }

  void _onNewLog() {
    if (_autoScroll && _scrollController.hasClients) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollController.hasClients) {
          _scrollController.animateTo(
            _scrollController.position.maxScrollExtent,
            duration: const Duration(milliseconds: 150),
            curve: Curves.easeOut,
          );
        }
      });
    }
  }

  void _copyAllLogs() {
    final allText = _processService.logs.map((e) => e.text).join('\n');
    Clipboard.setData(ClipboardData(text: allText));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Все логи скопированы в буфер обмена'),
        duration: Duration(seconds: 2),
        backgroundColor: Color(0xFF6366F1),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final logs = _processService.logs;
    final filterText = _filterController.text.trim().toLowerCase();

    final filteredLogs = logs.where((entry) {
      if (_selectedLevel != null && entry.level != _selectedLevel) {
        return false;
      }
      if (filterText.isNotEmpty &&
          !entry.text.toLowerCase().contains(filterText)) {
        return false;
      }
      return true;
    }).toList();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0),
      child: Column(
        children: [
          // Toolbar
          _buildToolbar(),
          const SizedBox(height: 16),

          // Terminal Window
          Expanded(
            child: Container(
              decoration: BoxDecoration(
                color: const Color(0xFF0B0E17),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: const Color(0xFF1E293B),
                  width: 1.5,
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.5),
                    blurRadius: 20,
                    offset: const Offset(0, 10),
                  ),
                ],
              ),
              child: Column(
                children: [
                  // Terminal Header Bar
                  _buildTerminalHeader(filteredLogs.length, logs.length),

                  // Log List
                  Expanded(
                    child: filteredLogs.isEmpty
                        ? const Center(
                            child: Text(
                              'Логи отсутствуют или отфильтрованы',
                              style: TextStyle(
                                color: Color(0xFF64748B),
                                fontSize: 14,
                              ),
                            ),
                          )
                        : ListView.builder(
                            controller: _scrollController,
                            padding: const EdgeInsets.all(16),
                            itemCount: filteredLogs.length,
                            itemBuilder: (context, index) {
                              return _buildLogLine(
                                filteredLogs[index],
                                index + 1,
                              );
                            },
                          ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildToolbar() {
    return Row(
      children: [
        // Search text
        Expanded(
          child: TextField(
            controller: _filterController,
            style: const TextStyle(color: Colors.white, fontSize: 13),
            decoration: InputDecoration(
              hintText: 'Поиск по логам...',
              hintStyle: const TextStyle(color: Color(0xFF64748B)),
              prefixIcon: const Icon(Icons.search_rounded, color: Color(0xFF818CF8), size: 20),
              suffixIcon: _filterController.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear_rounded, size: 18),
                      onPressed: () => setState(() => _filterController.clear()),
                    )
                  : null,
              filled: true,
              fillColor: const Color(0xFF161A29),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFF282F48)),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Color(0xFF282F48)),
              ),
            ),
            onChanged: (_) => setState(() {}),
          ),
        ),
        const SizedBox(width: 12),

        // Filter chips
        _buildLevelChip(null, 'Все'),
        const SizedBox(width: 6),
        _buildLevelChip(LogLevel.error, 'Errors', const Color(0xFFEF4444)),
        const SizedBox(width: 6),
        _buildLevelChip(LogLevel.warning, 'Warnings', const Color(0xFFF59E0B)),
        const SizedBox(width: 6),
        _buildLevelChip(LogLevel.fabric, 'Fabric', const Color(0xFF8B5CF6)),
        const SizedBox(width: 12),

        // Actions: Autoscroll, Copy, Clear
        IconButton(
          onPressed: () => setState(() => _autoScroll = !_autoScroll),
          icon: Icon(
            _autoScroll ? Icons.vertical_align_bottom_rounded : Icons.pause_circle_outline_rounded,
            color: _autoScroll ? const Color(0xFF10B981) : const Color(0xFF94A3B8),
          ),
          tooltip: _autoScroll ? 'Автопрокрутка включена' : 'Автопрокрутка отключена',
        ),
        IconButton(
          onPressed: _copyAllLogs,
          icon: const Icon(Icons.copy_rounded, color: Color(0xFF94A3B8), size: 20),
          tooltip: 'Копировать всё',
        ),
        IconButton(
          onPressed: () => setState(() => _processService.clearLogs()),
          icon: const Icon(Icons.delete_sweep_rounded, color: Color(0xFFEF4444), size: 20),
          tooltip: 'Очистить логи',
        ),
      ],
    );
  }

  Widget _buildLevelChip(LogLevel? level, String label, [Color? color]) {
    final isSelected = _selectedLevel == level;
    final activeColor = color ?? const Color(0xFF6366F1);

    return InkWell(
      onTap: () => setState(() => _selectedLevel = level),
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? activeColor.withOpacity(0.25) : const Color(0xFF161A29),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isSelected ? activeColor : const Color(0xFF282F48),
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
            color: isSelected ? activeColor : const Color(0xFF94A3B8),
          ),
        ),
      ),
    );
  }

  Widget _buildTerminalHeader(int shownCount, int totalCount) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      decoration: const BoxDecoration(
        color: Color(0xFF131724),
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        border: Border(bottom: BorderSide(color: Color(0xFF1E293B))),
      ),
      child: Row(
        children: [
          // Mac-like terminal dots
          Container(width: 10, height: 10, decoration: const BoxDecoration(color: Color(0xFFEF4444), shape: BoxShape.circle)),
          const SizedBox(width: 6),
          Container(width: 10, height: 10, decoration: const BoxDecoration(color: Color(0xFFF59E0B), shape: BoxShape.circle)),
          const SizedBox(width: 6),
          Container(width: 10, height: 10, decoration: const BoxDecoration(color: Color(0xFF10B981), shape: BoxShape.circle)),
          const SizedBox(width: 16),
          const Text(
            'Live Console Output',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.bold,
              color: Color(0xFF94A3B8),
              fontFamily: 'Consolas, monospace',
            ),
          ),
          const Spacer(),
          Text(
            'Строк: $shownCount / $totalCount',
            style: const TextStyle(
              fontSize: 11,
              color: Color(0xFF64748B),
              fontFamily: 'Consolas, monospace',
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLogLine(LogEntry entry, int lineNumber) {
    Color textColor = const Color(0xFFE2E8F0);
    Color tagColor = const Color(0xFF64748B);
    String tag = 'INFO';

    switch (entry.level) {
      case LogLevel.error:
        textColor = const Color(0xFFF87171);
        tagColor = const Color(0xFFEF4444);
        tag = 'ERROR';
        break;
      case LogLevel.warning:
        textColor = const Color(0xFFFCD34D);
        tagColor = const Color(0xFFF59E0B);
        tag = 'WARN';
        break;
      case LogLevel.fabric:
        textColor = const Color(0xFFC084FC);
        tagColor = const Color(0xFFA855F7);
        tag = 'FABRIC';
        break;
      case LogLevel.gradle:
        textColor = const Color(0xFF38BDF8);
        tagColor = const Color(0xFF0284C7);
        tag = 'GRADLE';
        break;
      case LogLevel.system:
        textColor = const Color(0xFF34D399);
        tagColor = const Color(0xFF10B981);
        tag = 'SYS';
        break;
      case LogLevel.info:
      default:
        textColor = const Color(0xFFCBD5E1);
        tagColor = const Color(0xFF64748B);
        tag = 'INFO';
    }

    final timeStr = '${entry.timestamp.hour.toString().padLeft(2, '0')}:'
        '${entry.timestamp.minute.toString().padLeft(2, '0')}:'
        '${entry.timestamp.second.toString().padLeft(2, '0')}';

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Line Number
          SizedBox(
            width: 40,
            child: Text(
              lineNumber.toString(),
              style: const TextStyle(
                color: Color(0xFF475569),
                fontSize: 12,
                fontFamily: 'Consolas, monospace',
              ),
            ),
          ),
          // Time
          Text(
            timeStr,
            style: const TextStyle(
              color: Color(0xFF64748B),
              fontSize: 12,
              fontFamily: 'Consolas, monospace',
            ),
          ),
          const SizedBox(width: 8),
          // Tag
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
            decoration: BoxDecoration(
              color: tagColor.withOpacity(0.18),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              tag,
              style: TextStyle(
                color: tagColor,
                fontSize: 10,
                fontWeight: FontWeight.bold,
                fontFamily: 'Consolas, monospace',
              ),
            ),
          ),
          const SizedBox(width: 10),
          // Log Text
          Expanded(
            child: SelectableText(
              entry.text,
              style: TextStyle(
                color: textColor,
                fontSize: 12.5,
                fontFamily: 'Consolas, monospace',
                height: 1.4,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
