import 'package:flutter/material.dart';
import '../../services/platform_service.dart';

class AddGuardiansScreen extends StatefulWidget {
  const AddGuardiansScreen({super.key});

  @override
  State<AddGuardiansScreen> createState() => _AddGuardiansScreenState();
}

class _AddGuardiansScreenState extends State<AddGuardiansScreen> {
  List<String> _registeredFaces = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadFaces();
  }

  Future<void> _loadFaces() async {
    final faces = await PlatformService.getRegisteredFaces();
    setState(() {
      _registeredFaces = faces;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Additional Guardians')),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
            Text(
              'Add More Guardians',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              'You can register up to 4 faces total. Additional guardians can also unlock restricted apps.',
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),

            // Registered faces list
            if (_isLoading)
              const Center(child: CircularProgressIndicator())
            else ...[
              Text(
                'Registered Faces (${_registeredFaces.length}/4)',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
              ),
              const SizedBox(height: 12),
              ..._registeredFaces.asMap().entries.map((entry) => Card(
                    child: ListTile(
                      leading: CircleAvatar(
                        child: Text('${entry.key + 1}'),
                      ),
                      title: Text(entry.value),
                      trailing: entry.key == 0
                          ? Chip(
                              label: const Text('Primary'),
                              backgroundColor: Theme.of(context)
                                  .colorScheme
                                  .primaryContainer,
                            )
                          : IconButton(
                              icon: const Icon(Icons.delete_outline),
                              onPressed: () => _deleteFace(entry.key),
                            ),
                    ),
                  )),
            ],

                  ],
                ),
              ),
            ),

            // Bottom buttons
            Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  if (_registeredFaces.length < 4)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: SizedBox(
                        width: double.infinity,
                        height: 48,
                        child: OutlinedButton.icon(
                          onPressed: () async {
                            await Navigator.pushNamed(
                              context,
                              '/onboarding/face-registration',
                            );
                            _loadFaces();
                          },
                          icon: const Icon(Icons.person_add),
                          label: const Text('Add Another Guardian'),
                        ),
                      ),
                    ),
                  SizedBox(
                    width: double.infinity,
                    height: 56,
                    child: FilledButton(
                      onPressed: () {
                        Navigator.pushNamed(context, '/onboarding/pin-setup');
                      },
                      child: const Text('Continue', style: TextStyle(fontSize: 16)),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _deleteFace(int index) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Remove Guardian?'),
        content: Text(
          'Remove ${_registeredFaces[index]} from authorized faces?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Remove'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await PlatformService.deleteRegisteredFace(index);
      _loadFaces();
    }
  }
}
