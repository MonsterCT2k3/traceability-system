import 'package:flutter/material.dart';
import '../../domain/entities/trace_entity.dart';
import '../../domain/entities/trace_history_event_entity.dart';

class TraceTimeline extends StatelessWidget {
  final TraceEntity trace;

  const TraceTimeline({super.key, required this.trace});

  @override
  Widget build(BuildContext context) {
    if (trace.historyEvents.isEmpty) {
      return const Text('Chưa có thông tin nhật ký',
          style: TextStyle(color: Colors.black54, fontStyle: FontStyle.italic));
    }

    final rawBatchEvents = trace.historyEvents.where((e) => e.eventType.startsWith('RAW_BATCH')).toList();
    final mainEvents = trace.historyEvents.where((e) => !e.eventType.startsWith('RAW_BATCH')).toList();

    return Column(
      children: [
        if (rawBatchEvents.isNotEmpty) ...[
          _buildRawBatchesCard(context, rawBatchEvents),
          if (mainEvents.isNotEmpty) _buildDownArrow(),
        ],
        ...mainEvents.asMap().entries.map((entry) {
          int idx = entry.key;
          var event = entry.value;

          return Column(
            children: [
              _buildMainEventCard(context, event),
              if (idx < mainEvents.length - 1) _buildDownArrow(),
            ],
          );
        }),
      ],
    );
  }

  Widget _buildDownArrow() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Icon(Icons.arrow_downward_rounded, color: Colors.blue.shade300, size: 36),
    );
  }

  Widget _buildRawBatchesCard(BuildContext context, List<TraceHistoryEventEntity> rawEvents) {
    final createdEvents = rawEvents.where((e) => e.eventType == 'RAW_BATCH_CREATED').toList();
    if (createdEvents.isEmpty) return const SizedBox.shrink();

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.blue.shade200, width: 1.5),
        boxShadow: [
          BoxShadow(color: Colors.blue.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.blue.shade50,
              borderRadius: const BorderRadius.vertical(top: Radius.circular(14)),
              border: Border(bottom: BorderSide(color: Colors.blue.shade100)),
            ),
            child: Row(
              children: [
                Icon(Icons.inventory_2_outlined, color: Colors.blue.shade700, size: 20),
                const SizedBox(width: 8),
                Text('Nguyên liệu đầu vào',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Colors.blue.shade900)),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Column(
              children: createdEvents.map((rawEvent) {
                return InkWell(
                  onTap: () => _showRawBatchDetails(context, rawEvent),
                  borderRadius: BorderRadius.circular(12),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 24,
                          backgroundColor: Colors.grey.shade100,
                          backgroundImage: (rawEvent.actorAvatarUrl != null && rawEvent.actorAvatarUrl!.isNotEmpty)
                              ? NetworkImage(rawEvent.actorAvatarUrl!)
                              : NetworkImage(
                                      'https://ui-avatars.com/api/?name=${Uri.encodeComponent(rawEvent.actorName ?? 'A')}&background=random&color=fff')
                                  as ImageProvider,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                rawEvent.eventDescription.replaceAll('Khai báo lô nguyên liệu gốc: ', ''),
                                style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14, color: Colors.black87),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                rawEvent.actorName ?? '',
                                style: TextStyle(color: Colors.grey.shade600, fontSize: 12),
                              ),
                            ],
                          ),
                        ),
                        Icon(Icons.chevron_right_rounded, size: 20, color: Colors.grey.shade400),
                      ],
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMainEventCard(BuildContext context, TraceHistoryEventEntity event) {
    String displayTime = event.timestamp ?? '';
    if (displayTime.isNotEmpty) {
      try {
        final d = DateTime.parse(displayTime).toLocal();
        displayTime = '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year} ${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
      } catch (_) {}
    }

    Widget? verificationBadge;
    if (event.isVerifiedOnChain == true) {
      final isBlockchain = event.eventType == 'PALLET_MANUFACTURED';
      verificationBadge = Container(
        margin: const EdgeInsets.only(top: 12),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: isBlockchain ? Colors.green.withOpacity(0.1) : Colors.blue.withOpacity(0.1),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: isBlockchain ? Colors.green.withOpacity(0.3) : Colors.blue.withOpacity(0.3)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(isBlockchain ? Icons.verified : Icons.admin_panel_settings,
                size: 16, color: isBlockchain ? Colors.green : Colors.blue),
            const SizedBox(width: 6),
            Text(
              isBlockchain ? 'Đã xác thực Blockchain' : 'Xác thực bởi Hệ thống',
              style: TextStyle(color: isBlockchain ? Colors.green : Colors.blue, fontSize: 12, fontWeight: FontWeight.bold),
            ),
          ],
        ),
      );
    } else if (event.isVerifiedOnChain == false) {
      verificationBadge = Container(
        margin: const EdgeInsets.only(top: 12),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: Colors.red.withOpacity(0.1),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: Colors.red.withOpacity(0.3)),
        ),
        child: const Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.warning_amber_rounded, size: 16, color: Colors.red),
            SizedBox(width: 6),
            Text('Dữ liệu không khớp Blockchain',
                style: TextStyle(color: Colors.red, fontSize: 12, fontWeight: FontWeight.bold)),
          ],
        ),
      );
    }

    final isManufacturer = event.eventType == 'PALLET_MANUFACTURED';
    final themeColor = isManufacturer ? Colors.blue : Colors.teal;

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: themeColor.withOpacity(0.2), width: 1.5),
        boxShadow: [
          BoxShadow(
            color: themeColor.withOpacity(0.08),
            blurRadius: 16,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header: Avatar + Tên đơn vị có Gradient nhẹ
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [themeColor.withOpacity(0.15), Colors.white],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                stops: const [0.0, 1.0],
              ),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(18)),
            ),
            child: Row(
              children: [
                // Khung viền màu sắc cho Avatar
                Container(
                  padding: const EdgeInsets.all(3),
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: LinearGradient(
                      colors: [themeColor.shade400, themeColor.shade100],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    boxShadow: [
                      BoxShadow(color: themeColor.withOpacity(0.2), blurRadius: 8, offset: const Offset(0, 4)),
                    ],
                  ),
                  child: CircleAvatar(
                    radius: 38,
                    backgroundColor: Colors.white,
                    backgroundImage: (event.actorAvatarUrl != null && event.actorAvatarUrl!.isNotEmpty)
                        ? NetworkImage(event.actorAvatarUrl!)
                        : NetworkImage(
                                'https://ui-avatars.com/api/?name=${Uri.encodeComponent(event.actorName ?? 'A')}&background=random&color=fff')
                            as ImageProvider,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        event.actorName ?? 'Hệ thống',
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17, color: themeColor.shade900),
                      ),
                      const SizedBox(height: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: themeColor.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: themeColor.withOpacity(0.2)),
                        ),
                        child: Text(
                          isManufacturer ? 'Nhà Sản Xuất' : 'Nhà Phân Phối / Bán Lẻ',
                          style: TextStyle(color: themeColor.shade700, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.5),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          // Body: Nội dung sự kiện
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  event.eventDescription,
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16, color: Colors.black87),
                ),
                const SizedBox(height: 16),
                if (displayTime.isNotEmpty) _buildInfoRow(Icons.access_time_rounded, displayTime),
                if (event.location != null && event.location!.isNotEmpty)
                  _buildInfoRow(Icons.location_on_rounded, event.location!),
                if (verificationBadge != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: verificationBadge,
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 16, color: Colors.grey.shade500),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              text,
              style: TextStyle(color: Colors.grey.shade700, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }

  void _showRawBatchDetails(BuildContext context, TraceHistoryEventEntity event) {
    String displayTime = event.timestamp ?? '';
    if (displayTime.isNotEmpty) {
      try {
        final d = DateTime.parse(displayTime).toLocal();
        displayTime = '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year} ${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
      } catch (_) {}
    }

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (context) {
        return Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  CircleAvatar(
                    radius: 48,
                    backgroundColor: Colors.grey.shade100,
                    backgroundImage: (event.actorAvatarUrl != null && event.actorAvatarUrl!.isNotEmpty)
                        ? NetworkImage(event.actorAvatarUrl!)
                        : NetworkImage('https://ui-avatars.com/api/?name=${Uri.encodeComponent(event.actorName ?? 'A')}&background=random&color=fff') as ImageProvider,
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(event.actorName ?? '', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                        const SizedBox(height: 2),
                        Text('Nhà cung cấp nguyên liệu', style: TextStyle(color: Colors.grey.shade600, fontSize: 14)),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              Text(event.eventDescription, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.blue)),
              const SizedBox(height: 16),
              if (displayTime.isNotEmpty) _buildDetailRow(Icons.access_time_rounded, 'Thời gian:', displayTime),
              if (event.location != null && event.location!.isNotEmpty) _buildDetailRow(Icons.location_on_outlined, 'Địa điểm:', event.location!),
              if (event.isVerifiedOnChain == true)
                Container(
                  margin: const EdgeInsets.only(top: 8),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(color: Colors.green.shade50, borderRadius: BorderRadius.circular(8)),
                  child: Row(
                    children: [
                      const Icon(Icons.verified, size: 18, color: Colors.green),
                      const SizedBox(width: 8),
                      const Expanded(child: Text('Đã xác thực trên Blockchain', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold, fontSize: 13))),
                    ],
                  ),
                ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.blue,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Đóng', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                ),
              )
            ],
          ),
        );
      },
    );
  }

  Widget _buildDetailRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 20, color: Colors.grey.shade600),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
                const SizedBox(height: 2),
                Text(value, style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 15, color: Colors.black87)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
