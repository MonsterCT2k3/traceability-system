import 'trace_history_event_entity.dart';

class TraceEntity {
  final String unitId;
  final String unitSerial;
  final String productId;
  final String productName;
  final String? productDescription;
  final String? productImageUrl;
  final String cartonCode;
  final String palletCode;
  final String palletName;
  final String palletManufacturedAt;
  final String? palletExpiryAt;
  final int scanCount;
  final List<TraceHistoryEventEntity> historyEvents;
  final bool? isDataIntact;
  final DirectTraceEntity? directTrace;

  TraceEntity({
    required this.unitId,
    required this.unitSerial,
    required this.productId,
    required this.productName,
    required this.productDescription,
    this.productImageUrl,
    required this.cartonCode,
    required this.palletCode,
    required this.palletName,
    required this.palletManufacturedAt,
    required this.palletExpiryAt,
    required this.scanCount,
    this.historyEvents = const [],
    this.isDataIntact,
    this.directTrace,
  });
}

class DirectTraceEntity {
  final TraceNodeEntity currentNode;
  final List<TraceNodeEntity> directInputs;
  final String verificationScope;
  final DirectVerificationSummaryEntity? verificationSummary;

  const DirectTraceEntity({
    required this.currentNode,
    required this.directInputs,
    required this.verificationScope,
    this.verificationSummary,
  });
}

class TraceNodeEntity {
  final String id;
  final String nodeType;
  final String code;
  final String name;
  final String? actorName;
  final String? actorAvatarUrl;
  final String? location;
  final String? occurredAt;
  final String? quantity;
  final String? unit;
  final String? note;
  final String? batchNo;
  final String? expiryAt;
  final String? packagingType;
  final String? processingMethod;
  final String? blockchainBatchIdHex;
  final bool hasInputs;
  final String verificationStatus;

  const TraceNodeEntity({
    required this.id,
    required this.nodeType,
    required this.code,
    required this.name,
    this.actorName,
    this.actorAvatarUrl,
    this.location,
    this.occurredAt,
    this.quantity,
    this.unit,
    this.note,
    this.batchNo,
    this.expiryAt,
    this.packagingType,
    this.processingMethod,
    this.blockchainBatchIdHex,
    required this.hasInputs,
    required this.verificationStatus,
  });
}

class DirectVerificationSummaryEntity {
  final String currentNodeStatus;
  final String inputRelationStatus;
  final int verifiedInputCount;
  final int totalInputCount;
  final String overallStatus;

  const DirectVerificationSummaryEntity({
    required this.currentNodeStatus,
    required this.inputRelationStatus,
    required this.verifiedInputCount,
    required this.totalInputCount,
    required this.overallStatus,
  });
}
