import 'package:equatable/equatable.dart';

class ScanHistoryEntity extends Equatable {
  final String id;
  final String unitSerial;
  final DateTime scannedAt;
  final String? productName;
  final String? productImage;

  const ScanHistoryEntity({
    required this.id,
    required this.unitSerial,
    required this.scannedAt,
    this.productName,
    this.productImage,
  });

  @override
  List<Object?> get props =>
      [id, unitSerial, scannedAt, productName, productImage];
}
