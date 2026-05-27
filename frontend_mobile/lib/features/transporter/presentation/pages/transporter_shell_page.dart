import 'package:flutter/material.dart';

import '../../../../core/widgets/trace_bottom_navigation.dart';
import '../../../main/presentation/pages/tabs/profile_tab.dart';
import 'transporter_orders_tab.dart';

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
        title: Text(_index == 0 ? 'Đơn Vận Chuyển' : 'Cá Nhân'),
        centerTitle: true,
      ),
      body: IndexedStack(
        index: _index,
        children: const [
          TransporterOrdersTab(),
          ProfileTab(),
        ],
      ),
      bottomNavigationBar: TraceBottomNavigation(
        selectedIndex: _index,
        onDestinationSelected: (index) => setState(() => _index = index),
        items: const [
          TraceBottomNavigationItem(
            icon: Icons.local_shipping_outlined,
            selectedIcon: Icons.local_shipping_rounded,
            label: 'Đơn hàng',
          ),
          TraceBottomNavigationItem(
            icon: Icons.person_outline_rounded,
            selectedIcon: Icons.person_rounded,
            label: 'Cá nhân',
          ),
        ],
      ),
    );
  }
}
