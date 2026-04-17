import 'package:flutter/material.dart';

import '../../../main/presentation/pages/tabs/profile_tab.dart';
import 'transporter_orders_tab.dart';

/// Giao diện chính dành cho tài khoản TRANSPORTER (sau đăng nhập).
class TransporterShellPage extends StatefulWidget {
  const TransporterShellPage({super.key});

  @override
  State<TransporterShellPage> createState() => _TransporterShellPageState();
}

class _TransporterShellPageState extends State<TransporterShellPage> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_index == 0 ? 'Đơn vận chuyển' : 'Cá nhân'),
        centerTitle: true,
      ),
      body: IndexedStack(
        index: _index,
        children: const [
          TransporterOrdersTab(),
          ProfileTab(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.local_shipping_outlined),
            selectedIcon: Icon(Icons.local_shipping),
            label: 'Đơn hàng',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Cá nhân',
          ),
        ],
      ),
    );
  }
}
