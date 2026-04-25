import 'package:flutter/material.dart';

import '../../../main/presentation/pages/tabs/profile_tab.dart';
import 'retailer_orders_tab.dart';

/// Giao diện chính dành cho tài khoản RETAILER.
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
        title: Text(_index == 0 ? 'Đặt hàng thành phẩm' : 'Cá nhân'),
        centerTitle: true,
      ),
      body: IndexedStack(
        index: _index,
        children: const [
          RetailerOrdersTab(),
          ProfileTab(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.shopping_bag_outlined),
            selectedIcon: Icon(Icons.shopping_bag),
            label: 'Đặt hàng',
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
