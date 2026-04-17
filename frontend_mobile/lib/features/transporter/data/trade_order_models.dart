/// Bản rút gọn đơn hàng từ product-service (JSON snake_case / camelCase tùy server — hỗ trợ cả hai).
class TradeOrderDto {
  final String id;
  final String orderCode;
  final String orderType;
  final String buyerId;
  final String sellerId;
  final String status;
  final String? carrierId;
  final String? note;
  final String? createdAt;
  final List<TradeOrderLineDto> lines;

  TradeOrderDto({
    required this.id,
    required this.orderCode,
    required this.orderType,
    required this.buyerId,
    required this.sellerId,
    required this.status,
    this.carrierId,
    this.note,
    this.createdAt,
    this.lines = const [],
  });

  factory TradeOrderDto.fromJson(Map<String, dynamic> json) {
    final rawLines = json['lines'];
    List<TradeOrderLineDto> parsed = [];
    if (rawLines is List) {
      parsed = rawLines
          .map((e) => TradeOrderLineDto.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    }
    return TradeOrderDto(
      id: json['id']?.toString() ?? '',
      orderCode: json['orderCode']?.toString() ?? '',
      orderType: json['orderType']?.toString() ?? '',
      buyerId: json['buyerId']?.toString() ?? '',
      sellerId: json['sellerId']?.toString() ?? '',
      status: json['status']?.toString() ?? '',
      carrierId: json['carrierId']?.toString(),
      note: json['note']?.toString(),
      createdAt: json['createdAt']?.toString(),
      lines: parsed,
    );
  }
}

class TradeOrderLineDto {
  final int? lineIndex;
  final String? targetRawBatchId;
  final String? quantityRequested;
  final String? unit;

  TradeOrderLineDto({
    this.lineIndex,
    this.targetRawBatchId,
    this.quantityRequested,
    this.unit,
  });

  factory TradeOrderLineDto.fromJson(Map<String, dynamic> json) {
    return TradeOrderLineDto(
      lineIndex: json['lineIndex'] is int ? json['lineIndex'] as int : int.tryParse('${json['lineIndex']}'),
      targetRawBatchId: json['targetRawBatchId']?.toString(),
      quantityRequested: json['quantityRequested']?.toString(),
      unit: json['unit']?.toString(),
    );
  }
}
