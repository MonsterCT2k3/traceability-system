import 'package:flutter/material.dart';

class ScanTab extends StatelessWidget {
  const ScanTab({super.key});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.qr_code_scanner, size: 120, color: Theme.of(context).colorScheme.primary),
          const SizedBox(height: 24),
          const Text(
            'Màn hình Quét mã QR',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          const Text(
            'Đưa camera vào mã QR trên sản phẩm.',
            style: TextStyle(fontSize: 16, color: Colors.grey),
          ),
          const SizedBox(height: 32),
          ElevatedButton.icon(
            onPressed: () {
              // TODO: Implement QR Scanner Camera
            },
            icon: const Icon(Icons.camera_alt),
            label: const Text('Bật Camera'),
          )
        ],
      ),
    );
  }
}
