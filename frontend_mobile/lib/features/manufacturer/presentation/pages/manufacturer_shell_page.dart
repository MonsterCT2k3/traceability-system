import 'package:flutter/material.dart';

import '../../../../core/widgets/trace_bottom_navigation.dart';
import '../../../main/presentation/pages/tabs/profile_tab.dart';
import 'banknote_serials_tab.dart';

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
        title: Text(_index == 0 ? 'Quản Lý Seri Sản Phẩm' : 'Cá Nhân'),
        centerTitle: true,
      ),
      body: IndexedStack(
        index: _index,
        children: const [
          BanknoteSerialsTab(),
          ProfileTab(),
        ],
      ),
      bottomNavigationBar: TraceBottomNavigation(
        selectedIndex: _index,
        onDestinationSelected: (index) => setState(() => _index = index),
        items: const [
          TraceBottomNavigationItem(
            icon: Icons.qr_code_2_outlined,
            selectedIcon: Icons.qr_code_2_rounded,
            label: 'Mã seri',
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
