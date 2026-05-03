import 'trace_history_event_entity.dart';

class TraceEntity {
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
  final List<TraceHistoryEventEntity> historyEvents;
  final bool? isDataIntact;

  TraceEntity({
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
    this.isDataIntact,
  });
}
