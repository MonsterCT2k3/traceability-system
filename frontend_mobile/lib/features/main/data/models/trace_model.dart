import '../../domain/entities/trace_entity.dart';
import 'trace_history_event_model.dart';

class TraceModel extends TraceEntity {
  TraceModel({
    required super.unitId,
    required super.unitSerial,
    required super.productId,
    required super.productName,
    super.productDescription,
    super.productImageUrl,
    required super.cartonCode,
    required super.palletCode,
    required super.palletName,
    required super.palletManufacturedAt,
    super.palletExpiryAt,
    required super.scanCount,
    super.historyEvents = const [],
    super.isDataIntact,
    super.directTrace,
  });

  factory TraceModel.fromJson(Map<String, dynamic> json) {
    return TraceModel(
      unitId: json['unitId']?.toString() ?? '',
      unitSerial: json['unitSerial']?.toString() ?? '',
      productId: json['productId']?.toString() ?? '',
      productName: json['productName']?.toString() ?? 'Không rõ',
      productDescription: json['productDescription']?.toString(),
      productImageUrl: json['productImageUrl']?.toString(),
      cartonCode: json['cartonCode']?.toString() ?? '',
      palletCode: json['palletCode']?.toString() ?? '',
      palletName: json['palletName']?.toString() ?? '',
      palletManufacturedAt: json['palletManufacturedAt']?.toString() ?? '',
      palletExpiryAt: json['palletExpiryAt']?.toString(),
      scanCount: json['scanCount'] is int
          ? json['scanCount'] as int
          : int.tryParse('${json['scanCount']}') ?? 0,
      historyEvents: (json['historyEvents'] as List<dynamic>?)
              ?.map((e) =>
                  TraceHistoryEventModel.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      isDataIntact: json['isDataIntact'] as bool?,
      directTrace: json['directTrace'] is Map<String, dynamic>
          ? _directTraceFromJson(json['directTrace'] as Map<String, dynamic>)
          : null,
    );
  }
}

DirectTraceEntity _directTraceFromJson(Map<String, dynamic> json) {
  return DirectTraceEntity(
    currentNode: _traceNodeFromJson(
        Map<String, dynamic>.from(json['currentNode'] as Map)),
    directInputs: (json['directInputs'] as List<dynamic>? ?? const [])
        .map((e) => _traceNodeFromJson(Map<String, dynamic>.from(e as Map)))
        .toList(),
    verificationScope:
        json['verificationScope']?.toString() ?? 'CURRENT_AND_DIRECT_INPUTS',
    verificationSummary: json['verificationSummary'] is Map
        ? _summaryFromJson(
            Map<String, dynamic>.from(json['verificationSummary'] as Map))
        : null,
  );
}

TraceNodeEntity _traceNodeFromJson(Map<String, dynamic> json) {
  return TraceNodeEntity(
    id: json['id']?.toString() ?? '',
    nodeType: json['nodeType']?.toString() ?? '',
    code: json['code']?.toString() ?? '',
    name: json['name']?.toString() ?? '',
    actorName: json['actorName']?.toString(),
    actorAvatarUrl: json['actorAvatarUrl']?.toString(),
    location: json['location']?.toString(),
    occurredAt: json['occurredAt']?.toString(),
    quantity: json['quantity']?.toString(),
    unit: json['unit']?.toString(),
    note: json['note']?.toString(),
    batchNo: json['batchNo']?.toString(),
    expiryAt: json['expiryAt']?.toString(),
    packagingType: json['packagingType']?.toString(),
    processingMethod: json['processingMethod']?.toString(),
    blockchainBatchIdHex: json['blockchainBatchIdHex']?.toString(),
    hasInputs: json['hasInputs'] == true,
    verificationStatus:
        json['verificationStatus']?.toString() ?? 'NOT_VERIFIED',
  );
}

DirectVerificationSummaryEntity _summaryFromJson(Map<String, dynamic> json) {
  return DirectVerificationSummaryEntity(
    currentNodeStatus: json['currentNodeStatus']?.toString() ?? 'NOT_VERIFIED',
    inputRelationStatus:
        json['inputRelationStatus']?.toString() ?? 'NOT_VERIFIED',
    verifiedInputCount: int.tryParse('${json['verifiedInputCount']}') ?? 0,
    totalInputCount: int.tryParse('${json['totalInputCount']}') ?? 0,
    overallStatus: json['overallStatus']?.toString() ?? 'NOT_VERIFIED',
  );
}

DirectTraceEntity directTraceFromJson(Map<String, dynamic> json) =>
    _directTraceFromJson(json);
