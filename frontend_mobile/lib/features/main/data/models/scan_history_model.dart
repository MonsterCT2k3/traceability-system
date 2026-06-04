import '../../domain/entities/scan_history_entity.dart';

class ScanHistoryModel extends ScanHistoryEntity {
  const ScanHistoryModel({
    required super.id,
    required super.unitSerial,
    required super.scannedAt,
    super.productName,
    super.productImage,
  });

  factory ScanHistoryModel.fromJson(Map<String, dynamic> json) {
    return ScanHistoryModel(
      id: json['id'] ?? '',
      unitSerial: json['unitSerial'] ?? '',
      scannedAt: json['scannedAt'] != null
          ? DateTime.parse(json['scannedAt'])
          : DateTime.now(),
      productName: json['productName'],
      productImage: json['productImage'],
    );
  }
}
