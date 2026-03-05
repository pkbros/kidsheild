import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import 'providers/app_state.dart';
import 'screens/onboarding/welcome_screen.dart';
import 'screens/onboarding/face_registration_screen.dart';
import 'screens/onboarding/add_guardians_screen.dart';
import 'screens/onboarding/pin_setup_screen.dart';
import 'screens/onboarding/permission_screen.dart';
import 'screens/onboarding/app_selection_screen.dart';
import 'screens/dashboard/dashboard_screen.dart';
import 'screens/dashboard/weekly_summary_screen.dart';
import 'screens/settings/settings_screen.dart';
import 'screens/settings/manage_faces_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  runApp(const KidShieldApp());
}

class KidShieldApp extends StatelessWidget {
  const KidShieldApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AppState()..initialize(),
      child: MaterialApp(
        title: 'KidShield',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xFF2196F3),
            brightness: Brightness.light,
          ),
          useMaterial3: true,
          appBarTheme: const AppBarTheme(
            centerTitle: true,
            elevation: 0,
          ),
        ),
        home: const _AppRouter(),
        routes: {
          '/welcome': (context) => const WelcomeScreen(),
          '/onboarding/face-registration': (context) =>
              const FaceRegistrationScreen(),
          '/onboarding/add-guardians': (context) =>
              const AddGuardiansScreen(),
          '/onboarding/pin-setup': (context) => const PinSetupScreen(),
          '/onboarding/permissions': (context) => const PermissionScreen(),
          '/onboarding/app-selection': (context) =>
              const AppSelectionScreen(isOnboarding: true),
          '/dashboard': (context) => const DashboardScreen(),
          '/settings': (context) => const SettingsScreen(),
          '/manage-apps': (context) =>
              const AppSelectionScreen(isOnboarding: false),
          '/manage-faces': (context) => const ManageFacesScreen(),
          '/permission-check': (context) => const PermissionScreen(),
          '/weekly-summary': (context) => const WeeklySummaryScreen(),
        },
      ),
    );
  }
}

/// Decides whether to show onboarding or dashboard based on setup state.
class _AppRouter extends StatelessWidget {
  const _AppRouter();

  @override
  Widget build(BuildContext context) {
    return Consumer<AppState>(
      builder: (context, appState, _) {
        if (appState.isLoading) {
          return const Scaffold(
            body: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.shield, size: 64, color: Colors.blue),
                  SizedBox(height: 16),
                  Text(
                    'KidShield',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  SizedBox(height: 24),
                  CircularProgressIndicator(),
                ],
              ),
            ),
          );
        }

        if (appState.isSetupComplete) {
          return const DashboardScreen();
        }

        return const WelcomeScreen();
      },
    );
  }
}
