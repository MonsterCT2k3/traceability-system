import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

/// Quét mã vạch / QR chứa chuỗi seri (in sẵn trên nhãn thùng hoặc quy ước nội bộ).
class BanknoteBarcodeScanPage extends StatefulWidget {
  const BanknoteBarcodeScanPage({super.key});

  @override
  State<BanknoteBarcodeScanPage> createState() => _BanknoteBarcodeScanPageState();
}

class _BanknoteBarcodeScanPageState extends State<BanknoteBarcodeScanPage> {
  bool _done = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Quét mã seri'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => Navigator.of(context).pop<String>(),
        ),
      ),
      body: Stack(
        children: [
          MobileScanner(
            onDetect: (capture) {
              if (_done) return;
              for (final b in capture.barcodes) {
                final v = b.rawValue;
                if (v != null && v.trim().isNotEmpty) {
                  _done = true;
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    if (context.mounted) Navigator.of(context).pop(v.trim());
                  });
                  return;
                }
              }
            },
          ),
          Positioned(
            left: 16,
            right: 16,
            bottom: 32,
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Text(
                  'Đưa mã vạch / QR vào khung. Seri in trực tiếp trên tờ tiền không phải mã vạch — dùng Nhập tay ở màn trước.',
                  style: TextStyle(color: Colors.grey.shade800, fontSize: 13),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
