import 'package:flutter/material.dart';

class HistoryTab extends StatelessWidget {
  const HistoryTab({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: Text(
        'Lịch sử (Tạm thời trống)',
        style: TextStyle(fontSize: 18, color: Colors.grey),
      ),
    );
  }
}
