import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';
import '../../../auth/presentation/bloc/auth_state.dart';
import 'tabs/history_tab.dart';
import 'tabs/scan_tab.dart';
import 'tabs/profile_tab.dart';

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
        setState(() {
          _currentIndex = 0;
        });
      },
      builder: (context, authState) {
        final isAuthenticated = authState is AuthAuthenticated;

        final tabs = isAuthenticated
            ? const [ScanTab(), HistoryTab(), ProfileTab()]
            : const [ScanTab(), ProfileTab()];

        final destinations = isAuthenticated
            ? const [
                NavigationDestination(
                  icon: Icon(Icons.qr_code_scanner),
                  selectedIcon: Icon(Icons.qr_code_scanner, color: Colors.green),
                  label: 'Quét QR',
                ),
                NavigationDestination(
                  icon: Icon(Icons.history_outlined),
                  selectedIcon: Icon(Icons.history),
                  label: 'Lịch sử',
                ),
                NavigationDestination(
                  icon: Icon(Icons.person_outline),
                  selectedIcon: Icon(Icons.person),
                  label: 'Cá nhân',
                ),
              ]
            : const [
                NavigationDestination(
                  icon: Icon(Icons.qr_code_scanner),
                  selectedIcon: Icon(Icons.qr_code_scanner, color: Colors.green),
                  label: 'Quét QR',
                ),
                NavigationDestination(
                  icon: Icon(Icons.person_outline),
                  selectedIcon: Icon(Icons.person),
                  label: 'Cá nhân',
                ),
              ];

        // Đảm bảo currentIndex không bị vọt quá giới hạn khi chuyển từ đã đăng nhập -> chưa đăng nhập
        int validIndex = _currentIndex;
        if (validIndex >= tabs.length) {
          validIndex = tabs.length - 1;
        }

        return Scaffold(
          body: tabs[validIndex],
          bottomNavigationBar: NavigationBar(
            selectedIndex: validIndex,
            onDestinationSelected: (index) {
              setState(() {
                _currentIndex = index;
              });
            },
            destinations: destinations,
          ),
        );
      },
    );
  }
}
