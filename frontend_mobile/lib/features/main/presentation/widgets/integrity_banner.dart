import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/entities/trace_entity.dart';
import '../bloc/trace/trace_bloc.dart';
import '../bloc/trace/trace_event.dart';

class IntegrityBanner extends StatelessWidget {
  final TraceEntity trace;
  final bool isVerifying;
  final String? verificationError;

  const IntegrityBanner({
    super.key,
    required this.trace,
    required this.isVerifying,
    this.verificationError,
  });

  @override
  Widget build(BuildContext context) {
    if (verificationError != null) {
      return Container(
        margin: const EdgeInsets.only(bottom: 20),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.orange.withOpacity(0.1),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.orange.withOpacity(0.3)),
        ),
        child: Row(
          children: [
            const Icon(Icons.info_outline, color: Colors.orange),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                verificationError!,
                style: const TextStyle(color: Colors.orange, fontWeight: FontWeight.w500),
              ),
            ),
            IconButton(
              icon: const Icon(Icons.refresh, color: Colors.orange, size: 20),
              onPressed: () {
                context.read<TraceBloc>().add(VerifyTraceBlockchain(trace.unitSerial));
              },
            ),
          ],
        ),
      );
    }

    if (trace.isDataIntact == null) {
      return Container(
        margin: const EdgeInsets.only(bottom: 20),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.blue.withOpacity(0.1),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.blue.withOpacity(0.3)),
        ),
        child: const Row(
          children: [
            SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
            SizedBox(width: 12),
            Expanded(
              child: Text(
                'Đang đối chiếu dữ liệu với Blockchain...',
                style: TextStyle(color: Colors.blue, fontWeight: FontWeight.w500),
              ),
            ),
          ],
        ),
      );
    }

    final isIntact = trace.isDataIntact!;
    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: isIntact ? Colors.green.withOpacity(0.1) : Colors.red.withOpacity(0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: isIntact ? Colors.green : Colors.red),
      ),
      child: Row(
        children: [
          Icon(
            isIntact ? Icons.verified_user : Icons.gpp_bad,
            color: isIntact ? Colors.green : Colors.red,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              isIntact
                  ? 'Dữ liệu đã được kiểm chứng an toàn bởi Blockchain'
                  : 'Cảnh báo: Phát hiện dữ liệu có dấu hiệu bị thay đổi!',
              style: TextStyle(
                color: isIntact ? Colors.green : Colors.red,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
