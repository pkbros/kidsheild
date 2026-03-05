import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/app_state.dart';
import '../../services/platform_service.dart';

class ManageFacesScreen extends StatefulWidget {
  const ManageFacesScreen({super.key});

  @override
  State<ManageFacesScreen> createState() => _ManageFacesScreenState();
}

class _ManageFacesScreenState extends State<ManageFacesScreen> {
  List<String> _faces = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadFaces();
  }

  Future<void> _loadFaces() async {
    final faces = await PlatformService.getRegisteredFaces();
    setState(() {
      _faces = faces;
      _isLoading = false;
    });
  }

  Future<void> _deleteFace(int index) async {
    if (_faces.length <= 1) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Cannot delete the last registered face. At least one face is required.'),
        ),
      );
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Remove Face?'),
        content: Text('Remove "${_faces[index]}" from authorized faces?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Remove'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await PlatformService.deleteRegisteredFace(index);
      if (!mounted) return;
      context.read<AppState>().refreshRegisteredFaces();
      _loadFaces();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Manage Faces')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(16),
                  color: Theme.of(context)
                      .colorScheme
                      .primaryContainer
                      .withValues(alpha: 0.3),
                  child: Text(
                    '${_faces.length}/4 face profiles registered',
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                ),
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: _faces.length,
                    itemBuilder: (context, index) {
                      return Card(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: ListTile(
                          leading: CircleAvatar(
                            backgroundColor: Theme.of(context)
                                .colorScheme
                                .primaryContainer,
                            child: Icon(
                              Icons.face,
                              color: Theme.of(context).colorScheme.primary,
                            ),
                          ),
                          title: Text(
                            _faces[index],
                            style:
                                const TextStyle(fontWeight: FontWeight.w600),
                          ),
                          subtitle: Text(
                            index == 0
                                ? 'Primary parent'
                                : 'Additional guardian',
                          ),
                          trailing: index == 0 && _faces.length == 1
                              ? const Chip(label: Text('Primary'))
                              : IconButton(
                                  icon: const Icon(Icons.delete_outline,
                                      color: Colors.red),
                                  onPressed: () => _deleteFace(index),
                                ),
                        ),
                      );
                    },
                  ),
                ),
                if (_faces.length < 4)
                  Padding(
                    padding: const EdgeInsets.all(16),
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
                          if (!mounted) return;
                          context.read<AppState>().refreshRegisteredFaces();
                        },
                        icon: const Icon(Icons.person_add),
                        label: const Text('Add New Face'),
                      ),
                    ),
                  ),
              ],
            ),
    );
  }
}
