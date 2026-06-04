import 'package:flutter/material.dart';
import '../../domain/entities/trace_entity.dart';

class ProductInfoDetails extends StatelessWidget {
  final TraceEntity trace;

  const ProductInfoDetails({super.key, required this.trace});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    color: Colors.grey.shade100,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.grey.shade200),
                  ),
                  child: trace.productImageUrl != null &&
                          trace.productImageUrl!.isNotEmpty
                      ? ClipRRect(
                          borderRadius: BorderRadius.circular(12),
                          child: Image.network(
                            trace.productImageUrl!,
                            fit: BoxFit.cover,
                            errorBuilder: (context, error, stackTrace) =>
                                const Icon(Icons.inventory_2_outlined,
                                    size: 40, color: Colors.grey),
                          ),
                        )
                      : const Icon(Icons.inventory_2_outlined,
                          size: 40, color: Colors.grey),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        trace.productName,
                        style: const TextStyle(
                            fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 4),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.blue.shade50,
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.remove_red_eye,
                                size: 14, color: Colors.blue.shade700),
                            const SizedBox(width: 4),
                            Text(
                              '${trace.scanCount} lượt quét',
                              style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.blue.shade700,
                                  fontWeight: FontWeight.w600),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            const Divider(height: 1),
            const SizedBox(height: 16),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.blue.shade50,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.blue.shade200, width: 1.5),
              ),
              child: Column(
                children: [
                  const Text('SERI SẢN PHẨM',
                      style: TextStyle(
                          color: Colors.blue,
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                          letterSpacing: 1.2)),
                  const SizedBox(height: 8),
                  Text(
                    trace.unitSerial,
                    style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 24,
                        letterSpacing: 2,
                        color: Colors.black87),
                  ),
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.blue.shade100),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(Icons.security,
                            size: 20, color: Colors.green.shade600),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            'Quý khách vui lòng kiểm tra và so sánh số Seri này với số seri trên tờ tiền (hoặc tem) đính kèm để xác thực hàng chính hãng.',
                            style: TextStyle(
                                fontSize: 13,
                                color: Colors.grey.shade800,
                                height: 1.4),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            _buildDetailRow('Mã thùng chứa', trace.cartonCode),
            _buildDetailRow('Mã lô sản xuất', trace.palletCode),
            _buildDetailRow('Tên lô', trace.palletName),
            _buildDetailRow('Ngày sản xuất', trace.palletManufacturedAt),
            _buildDetailRow('Hạn sử dụng', trace.palletExpiryAt ?? '—'),
            if ((trace.productDescription ?? '').trim().isNotEmpty) ...[
              const SizedBox(height: 8),
              const Text('Mô tả:',
                  style: TextStyle(
                      fontWeight: FontWeight.w600, color: Colors.black87)),
              const SizedBox(height: 4),
              Text(
                trace.productDescription!.trim(),
                style: TextStyle(color: Colors.grey.shade700, fontSize: 14),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(String label, String value,
      {bool isHighlight = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(
              label,
              style: TextStyle(color: Colors.grey.shade600, fontSize: 14),
            ),
          ),
          Expanded(
            flex: 3,
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(
                color: isHighlight ? Colors.blue.shade800 : Colors.black87,
                fontWeight: isHighlight ? FontWeight.bold : FontWeight.w500,
                fontSize: 14,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
