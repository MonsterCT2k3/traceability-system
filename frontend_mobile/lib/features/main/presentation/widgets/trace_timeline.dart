import 'package:flutter/material.dart';
import '../../domain/entities/trace_entity.dart';

class TraceTimeline extends StatelessWidget {
  final TraceEntity trace;

  const TraceTimeline({super.key, required this.trace});

  @override
  Widget build(BuildContext context) {
    if (trace.historyEvents.isEmpty) {
      return const Text('Chưa có thông tin nhật ký',
          style: TextStyle(color: Colors.black54, fontStyle: FontStyle.italic));
    }

    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: trace.historyEvents.length,
      itemBuilder: (context, index) {
        final event = trace.historyEvents[index];
        final isLast = index == trace.historyEvents.length - 1;

        String displayTime = event.timestamp ?? '';
        if (displayTime.isNotEmpty) {
          try {
            final d = DateTime.parse(displayTime).toLocal();
            displayTime =
                '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year} ${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
          } catch (_) {}
        }

        Widget? verificationBadge;
        if (event.isVerifiedOnChain == true) {
          final isBlockchain =
              event.eventType == 'RAW_BATCH_CREATED' || event.eventType == 'PALLET_MANUFACTURED';
          verificationBadge = Container(
            margin: const EdgeInsets.only(top: 6),
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: isBlockchain ? Colors.green.withOpacity(0.1) : Colors.blue.withOpacity(0.1),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(
                  color: isBlockchain ? Colors.green.withOpacity(0.3) : Colors.blue.withOpacity(0.3)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  isBlockchain ? Icons.verified : Icons.admin_panel_settings,
                  size: 14,
                  color: isBlockchain ? Colors.green : Colors.blue,
                ),
                const SizedBox(width: 4),
                Text(
                  isBlockchain ? 'Đã xác thực Blockchain' : 'Xác thực bởi Hệ thống',
                  style: TextStyle(
                    color: isBlockchain ? Colors.green : Colors.blue,
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          );
        } else if (event.isVerifiedOnChain == false) {
          verificationBadge = Container(
            margin: const EdgeInsets.only(top: 6),
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.red.withOpacity(0.1),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: Colors.red.withOpacity(0.3)),
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.warning_amber_rounded, size: 14, color: Colors.red),
                SizedBox(width: 4),
                Text(
                  'Dữ liệu không khớp Blockchain',
                  style: TextStyle(color: Colors.red, fontSize: 11, fontWeight: FontWeight.bold),
                ),
              ],
            ),
          );
        }

        return IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SizedBox(
                width: 30,
                child: Column(
                  children: [
                    Container(
                      width: 12,
                      height: 12,
                      decoration: const BoxDecoration(
                        color: Colors.blue,
                        shape: BoxShape.circle,
                      ),
                      margin: const EdgeInsets.only(top: 4),
                    ),
                    if (!isLast)
                      Expanded(
                        child: Container(
                          width: 2,
                          color: Colors.blue.withOpacity(0.5),
                        ),
                      ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 24.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        event.eventDescription,
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                      ),
                      const SizedBox(height: 4),
                      if (displayTime.isNotEmpty)
                        Text(
                          displayTime,
                          style: const TextStyle(color: Colors.black54, fontSize: 13),
                        ),
                      if (event.location != null && event.location!.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(
                            'Địa điểm: ${event.location}',
                            style: const TextStyle(color: Colors.black87, fontSize: 13),
                          ),
                        ),
                      if (event.actorName != null && event.actorName!.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(
                            'Đơn vị: ${event.actorName}',
                            style: const TextStyle(color: Colors.black87, fontSize: 13),
                          ),
                        ),
                      if (verificationBadge != null) verificationBadge,
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
