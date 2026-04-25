import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'injection_container.dart' as di;
import 'features/auth/presentation/bloc/auth_bloc.dart';
import 'features/auth/presentation/bloc/auth_state.dart';
import 'features/main/presentation/pages/main_page.dart';
import 'features/transporter/presentation/pages/transporter_shell_page.dart';
import 'features/manufacturer/presentation/pages/manufacturer_shell_page.dart';
import 'features/retailer/presentation/pages/retailer_shell_page.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await di.init();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  static bool _isTransporterRole(String? role) {
    return role != null && role.trim().toUpperCase() == 'TRANSPORTER';
  }

  static bool _isManufacturerRole(String? role) {
    return role != null && role.trim().toUpperCase() == 'MANUFACTURER';
  }

  static bool _isRetailerRole(String? role) {
    return role != null && role.trim().toUpperCase() == 'RETAILER';
  }

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<AuthBloc>(
          create: (_) => di.sl<AuthBloc>(),
        ),
      ],
      child: MaterialApp(
        title: 'Traceability Mobile App',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.green),
          useMaterial3: true,
        ),
        home: BlocBuilder<AuthBloc, AuthState>(
          buildWhen: (prev, next) {
            if (prev.runtimeType != next.runtimeType) return true;
            if (prev is AuthAuthenticated && next is AuthAuthenticated) {
              return prev.user.role != next.user.role;
            }
            return false;
          },
          builder: (context, state) {
            if (state is AuthAuthenticated) {
              if (_isTransporterRole(state.user.role)) {
                return const TransporterShellPage();
              }
              if (_isManufacturerRole(state.user.role)) {
                return const ManufacturerShellPage();
              }
              if (_isRetailerRole(state.user.role)) {
                return const RetailerShellPage();
              }
            }
            return const MainPage();
          },
        ),
      ),
    );
  }
}
