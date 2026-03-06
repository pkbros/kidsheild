import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/app_state.dart';
import '../../services/platform_service.dart';

class TaskAssignmentScreen extends StatefulWidget {
  const TaskAssignmentScreen({super.key});

  @override
  State<TaskAssignmentScreen> createState() => _TaskAssignmentScreenState();
}

class _TaskAssignmentScreenState extends State<TaskAssignmentScreen> {
  List<Map<String, dynamic>> _tasks = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadTasks();
  }

  Future<void> _loadTasks() async {
    final tasks = await PlatformService.getTasks();
    if (mounted) {
      setState(() {
        _tasks = tasks;
        _isLoading = false;
      });
    }
  }

  Future<void> _addTask() async {
    final result = await showDialog<Map<String, String>>(
      context: context,
      builder: (ctx) => _TaskDialog(),
    );
    if (result != null) {
      await PlatformService.addTask(
        title: result['title']!,
        description: result['description'] ?? '',
        bonusMinutes: int.tryParse(result['bonus'] ?? '15') ?? 15,
      );
      await _loadTasks();
      if (mounted) context.read<AppState>().refreshTasks();
    }
  }

  Future<void> _editTask(Map<String, dynamic> task) async {
    final result = await showDialog<Map<String, String>>(
      context: context,
      builder: (ctx) => _TaskDialog(
        initialTitle: task['title'] as String,
        initialDescription: task['description'] as String? ?? '',
        initialBonus: (task['bonusMinutes'] as int).toString(),
      ),
    );
    if (result != null) {
      await PlatformService.updateTask(
        taskId: task['id'] as String,
        title: result['title']!,
        description: result['description'] ?? '',
        bonusMinutes: int.tryParse(result['bonus'] ?? '15') ?? 15,
      );
      await _loadTasks();
      if (mounted) context.read<AppState>().refreshTasks();
    }
  }

  Future<void> _deleteTask(String taskId) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete Task'),
        content: const Text('Remove this task?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      await PlatformService.removeTask(taskId);
      await _loadTasks();
      if (mounted) context.read<AppState>().refreshTasks();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Daily Tasks'),
        centerTitle: true,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _addTask,
        child: const Icon(Icons.add),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _tasks.isEmpty
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(32),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.task_alt, size: 64, color: Colors.grey[300]),
                        const SizedBox(height: 16),
                        Text(
                          'No tasks yet',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            color: Colors.grey[600],
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Add daily tasks that your child can complete to earn more screen time.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.grey[500]),
                        ),
                      ],
                    ),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: _tasks.length,
                  itemBuilder: (context, index) {
                    final task = _tasks[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: Colors.purple.withValues(alpha: 0.1),
                          child: const Icon(Icons.star, color: Colors.purple),
                        ),
                        title: Text(
                          task['title'] as String,
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            if ((task['description'] as String?)?.isNotEmpty ==
                                true)
                              Text(task['description'] as String),
                            Text(
                              '+${task['bonusMinutes']} min bonus',
                              style: TextStyle(
                                color: Colors.purple[700],
                                fontWeight: FontWeight.w500,
                                fontSize: 12,
                              ),
                            ),
                          ],
                        ),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: const Icon(Icons.edit, size: 20),
                              onPressed: () => _editTask(task),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete, size: 20,
                                  color: Colors.red),
                              onPressed: () =>
                                  _deleteTask(task['id'] as String),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}

class _TaskDialog extends StatefulWidget {
  final String initialTitle;
  final String initialDescription;
  final String initialBonus;

  const _TaskDialog({
    this.initialTitle = '',
    this.initialDescription = '',
    this.initialBonus = '15',
  });

  @override
  State<_TaskDialog> createState() => _TaskDialogState();
}

class _TaskDialogState extends State<_TaskDialog> {
  late final TextEditingController _titleCtrl;
  late final TextEditingController _descCtrl;
  late final TextEditingController _bonusCtrl;

  @override
  void initState() {
    super.initState();
    _titleCtrl = TextEditingController(text: widget.initialTitle);
    _descCtrl = TextEditingController(text: widget.initialDescription);
    _bonusCtrl = TextEditingController(text: widget.initialBonus);
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _descCtrl.dispose();
    _bonusCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isEdit = widget.initialTitle.isNotEmpty;
    return AlertDialog(
      title: Text(isEdit ? 'Edit Task' : 'Add Task'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _titleCtrl,
              decoration: const InputDecoration(
                labelText: 'Task Title',
                hintText: 'e.g., Do homework',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _descCtrl,
              decoration: const InputDecoration(
                labelText: 'Description (optional)',
                hintText: 'e.g., Complete math worksheet',
                border: OutlineInputBorder(),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _bonusCtrl,
              decoration: const InputDecoration(
                labelText: 'Bonus Minutes',
                border: OutlineInputBorder(),
                suffixText: 'min',
              ),
              keyboardType: TextInputType.number,
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: () {
            if (_titleCtrl.text.trim().isEmpty) return;
            Navigator.pop(context, {
              'title': _titleCtrl.text.trim(),
              'description': _descCtrl.text.trim(),
              'bonus': _bonusCtrl.text.trim(),
            });
          },
          child: Text(isEdit ? 'Save' : 'Add'),
        ),
      ],
    );
  }
}
