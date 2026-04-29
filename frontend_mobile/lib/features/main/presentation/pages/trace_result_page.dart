import 'package:flutter/material.dart';

class TraceHistoryEvent {
  final String eventType;
  final String eventDescription;
  final String? timestamp;
  final String? actorId;
  final String? actorName;
  final String? location;
  final String? txHash;

  TraceHistoryEvent({
    required this.eventType,
    required this.eventDescription,
    this.timestamp,
    this.actorId,
    this.actorName,
    this.location,
    this.txHash,
  });

  factory TraceHistoryEvent.fromApi(Map<String, dynamic> json) {
    return TraceHistoryEvent(
      eventType: json['eventType']?.toString() ?? '',
      eventDescription: json['eventDescription']?.toString() ?? '',
      timestamp: json['timestamp']?.toString(),
      actorId: json['actorId']?.toString(),
      actorName: json['actorName']?.toString(),
      location: json['location']?.toString(),
      txHash: json['txHash']?.toString(),
    );
  }
}

class TraceViewModel {
  final String unitId;
  final String unitSerial;
  final String productName;
  final String? productDescription;
  final String cartonCode;
  final String palletCode;
  final String palletName;
  final String palletManufacturedAt;
  final String? palletExpiryAt;
  final int scanCount;
  final List<TraceHistoryEvent> historyEvents;

  TraceViewModel({
    required this.unitId,
    required this.unitSerial,
    required this.productName,
    required this.productDescription,
    required this.cartonCode,
    required this.palletCode,
    required this.palletName,
    required this.palletManufacturedAt,
    required this.palletExpiryAt,
    required this.scanCount,
    this.historyEvents = const [],
  });

  factory TraceViewModel.fromApi(Map<String, dynamic> json) {
    return TraceViewModel(
      unitId: json['unitId']?.toString() ?? '',
      unitSerial: json['unitSerial']?.toString() ?? '',
      productName: json['productName']?.toString() ?? 'Không rõ',
      productDescription: json['productDescription']?.toString(),
      cartonCode: json['cartonCode']?.toString() ?? '',
      palletCode: json['palletCode']?.toString() ?? '',
      palletName: json['palletName']?.toString() ?? '',
      palletManufacturedAt: json['palletManufacturedAt']?.toString() ?? '',
      palletExpiryAt: json['palletExpiryAt']?.toString(),
      scanCount: json['scanCount'] is int
          ? json['scanCount'] as int
          : int.tryParse('${json['scanCount']}') ?? 0,
      historyEvents: (json['historyEvents'] as List<dynamic>?)
              ?.map((e) => TraceHistoryEvent.fromApi(e as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }
}

class TraceResultPage extends StatelessWidget {
  final TraceViewModel trace;

  const TraceResultPage({super.key, required this.trace});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Kết quả truy xuất'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
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
              
            const SizedBox(height: 24),
            const Text('Nhật ký chuỗi cung ứng',
                style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            if (trace.historyEvents.isEmpty)
              const Text('Chưa có thông tin nhật ký',
                  style: TextStyle(color: Colors.black54, fontStyle: FontStyle.italic))
            else
              _buildTimeline(),
          ],
        ),
      ),
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

  Widget _buildTimeline() {
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
            displayTime = '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year} ${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
          } catch (_) {}
        }

        return IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Cột vẽ đường kẻ và chấm tròn
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
              // Nội dung sự kiện
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
                      if (event.txHash != null && event.txHash!.isNotEmpty)
                        Container(
                          margin: const EdgeInsets.only(top: 6),
                          padding: const EdgeInsets.all(6),
                          decoration: BoxDecoration(
                            color: Colors.green.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(4),
                            border: Border.all(color: Colors.green.withOpacity(0.3)),
                          ),
                          child: Row(
                            children: [
                              const Icon(Icons.verified_user, size: 14, color: Colors.green),
                              const SizedBox(width: 4),
                              Expanded(
                                child: Text(
                                  'Blockchain Tx: ${event.txHash}',
                                  style: const TextStyle(color: Colors.green, fontSize: 11),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ),
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
