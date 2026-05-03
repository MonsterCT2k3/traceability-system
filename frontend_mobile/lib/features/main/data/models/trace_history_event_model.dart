import '../../domain/entities/trace_history_event_entity.dart';

class TraceHistoryEventModel extends TraceHistoryEventEntity {
  TraceHistoryEventModel({
    required super.eventType,
    required super.eventDescription,
    super.timestamp,
    super.actorId,
    super.actorName,
    super.location,
    super.txHash,
    super.isVerifiedOnChain,
  });

  factory TraceHistoryEventModel.fromJson(Map<String, dynamic> json) {
    return TraceHistoryEventModel(
      eventType: json['eventType']?.toString() ?? '',
      eventDescription: json['eventDescription']?.toString() ?? '',
      timestamp: json['timestamp']?.toString(),
      actorId: json['actorId']?.toString(),
      actorName: json['actorName']?.toString(),
      location: json['location']?.toString(),
      txHash: json['txHash']?.toString(),
      isVerifiedOnChain: json['isVerifiedOnChain'] as bool?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'eventType': eventType,
      'eventDescription': eventDescription,
      'timestamp': timestamp,
      'actorId': actorId,
      'actorName': actorName,
      'location': location,
      'txHash': txHash,
      'isVerifiedOnChain': isVerifiedOnChain,
    };
  }
}
