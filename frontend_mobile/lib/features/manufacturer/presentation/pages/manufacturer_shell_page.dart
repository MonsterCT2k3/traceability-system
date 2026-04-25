import 'package:flutter/material.dart';

import '../../../main/presentation/pages/tabs/profile_tab.dart';
import 'banknote_serials_tab.dart';

/// NSX: thu thập danh sách số seri tờ tiền (sẽ gắn với id sản phẩm sau này — hiện lưu local).
class ManufacturerShellPage extends StatefulWidget {
  const ManufacturerShellPage({super.key});

  @override
  State<ManufacturerShellPage> createState() => _ManufacturerShellPageState();
}

class _ManufacturerShellPageState extends State<ManufacturerShellPage> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_index == 0 ? 'Seri tờ tiền' : 'Cá nhân'),
        centerTitle: true,
      ),
      body: IndexedStack(
        index: _index,
        children: const [
          BanknoteSerialsTab(),
          ProfileTab(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.currency_exchange_outlined),
            selectedIcon: Icon(Icons.currency_exchange),
            label: 'Seri 1000đ',
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
