import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/widgets/trace_bottom_navigation.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';
import '../../../auth/presentation/bloc/auth_state.dart';
import 'tabs/history_tab.dart';
import 'tabs/profile_tab.dart';
import 'tabs/scan_tab.dart';

class MainPage extends StatefulWidget {
  const MainPage({super.key});

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  int _currentIndex = 0;

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<AuthBloc, AuthState>(
      listenWhen: (previous, current) {
        final wasAuth = previous is AuthAuthenticated;
        final isAuth = current is AuthAuthenticated;
        return wasAuth != isAuth;
      },
      listener: (context, state) {
        setState(() => _currentIndex = 0);
      },
      builder: (context, authState) {
        final isAuthenticated = authState is AuthAuthenticated;
        final tabs = isAuthenticated
            ? const [ScanTab(), HistoryTab(), ProfileTab()]
            : const [ScanTab(), ProfileTab()];
        final destinations = isAuthenticated
            ? const [
                TraceBottomNavigationItem(
                  icon: Icons.qr_code_scanner_outlined,
                  selectedIcon: Icons.qr_code_scanner_rounded,
                  label: 'Quét QR',
                ),
                TraceBottomNavigationItem(
                  icon: Icons.history_outlined,
                  selectedIcon: Icons.history_rounded,
                  label: 'Lịch sử',
                ),
                TraceBottomNavigationItem(
                  icon: Icons.person_outline_rounded,
                  selectedIcon: Icons.person_rounded,
                  label: 'Cá nhân',
                ),
              ]
            : const [
                TraceBottomNavigationItem(
                  icon: Icons.qr_code_scanner_outlined,
                  selectedIcon: Icons.qr_code_scanner_rounded,
                  label: 'Quét QR',
                ),
                TraceBottomNavigationItem(
                  icon: Icons.person_outline_rounded,
                  selectedIcon: Icons.person_rounded,
                  label: 'Cá nhân',
                ),
              ];

        final validIndex =
            _currentIndex < tabs.length ? _currentIndex : tabs.length - 1;

        return Scaffold(
          extendBody: true,
          body: tabs[validIndex],
          bottomNavigationBar: TraceBottomNavigation(
            selectedIndex: validIndex,
            onDestinationSelected: (index) =>
                setState(() => _currentIndex = index),
            items: destinations,
          ),
        );
      },
    );
  }
}
