import '../../domain/entities/trace_entity.dart';
import 'trace_history_event_model.dart';

class TraceModel extends TraceEntity {
  TraceModel({
    required super.unitId,
    required super.unitSerial,
    required super.productName,
    super.productDescription,
    required super.cartonCode,
    required super.palletCode,
    required super.palletName,
    required super.palletManufacturedAt,
    super.palletExpiryAt,
    required super.scanCount,
    super.historyEvents = const [],
    super.isDataIntact,
  });

  factory TraceModel.fromJson(Map<String, dynamic> json) {
    return TraceModel(
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
              ?.map((e) => TraceHistoryEventModel.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      isDataIntact: json['isDataIntact'] as bool?,
    );
  }
}
