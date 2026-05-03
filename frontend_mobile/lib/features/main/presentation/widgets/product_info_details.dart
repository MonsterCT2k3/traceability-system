import 'package:flutter/material.dart';
import '../../domain/entities/trace_entity.dart';

class ProductInfoDetails extends StatelessWidget {
  final TraceEntity trace;

  const ProductInfoDetails({super.key, required this.trace});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Expanded(
              child: Text('Thông tin chi tiết',
                  style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
            ),
            Chip(
              label: Text('Lượt quét: ${trace.scanCount}'),
              avatar: const Icon(Icons.remove_red_eye_outlined, size: 18),
            ),
          ],
        ),
        const SizedBox(height: 16),
        _line('Sản phẩm', trace.productName),
        _line('Seri', trace.unitSerial),
        _line('Mã đơn vị', trace.unitId),
        _line('Mã thùng', trace.cartonCode),
        _line('Mã lô', trace.palletCode),
        _line('Tên lô', trace.palletName),
        _line('Ngày sản xuất', trace.palletManufacturedAt),
        _line('Hạn dùng', trace.palletExpiryAt ?? '—'),
        if ((trace.productDescription ?? '').trim().isNotEmpty)
          _line('Mô tả', trace.productDescription!.trim()),
      ],
    );
  }

  Widget _line(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: RichText(
        text: TextSpan(
          style: const TextStyle(color: Colors.black87, fontSize: 15),
          children: [
            TextSpan(text: '$label: ', style: const TextStyle(fontWeight: FontWeight.w700)),
            TextSpan(text: value),
          ],
        ),
      ),
    );
  }
}
