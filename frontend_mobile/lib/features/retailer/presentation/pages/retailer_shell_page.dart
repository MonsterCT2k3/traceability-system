import 'package:flutter/material.dart';

import '../../../../core/widgets/trace_bottom_navigation.dart';
import '../../../main/presentation/pages/tabs/profile_tab.dart';
import 'retailer_orders_tab.dart';

class RetailerShellPage extends StatefulWidget {
  const RetailerShellPage({super.key});

  @override
  State<RetailerShellPage> createState() => _RetailerShellPageState();
}

class _RetailerShellPageState extends State<RetailerShellPage> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_index == 0 ? 'Đặt Hàng Thành Phẩm' : 'Cá Nhân'),
        centerTitle: true,
      ),
      body: IndexedStack(
        index: _index,
        children: const [
          RetailerOrdersTab(),
          ProfileTab(),
        ],
      ),
      bottomNavigationBar: TraceBottomNavigation(
        selectedIndex: _index,
        onDestinationSelected: (index) => setState(() => _index = index),
        items: const [
          TraceBottomNavigationItem(
            icon: Icons.shopping_bag_outlined,
            selectedIcon: Icons.shopping_bag_rounded,
            label: 'Đặt hàng',
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
