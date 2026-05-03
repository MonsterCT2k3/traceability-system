class TraceHistoryEventEntity {
  final String eventType;
  final String eventDescription;
  final String? timestamp;
  final String? actorId;
  final String? actorName;
  final String? location;
  final String? txHash;
  final bool? isVerifiedOnChain;

  TraceHistoryEventEntity({
    required this.eventType,
    required this.eventDescription,
    this.timestamp,
    this.actorId,
    this.actorName,
    this.location,
    this.txHash,
    this.isVerifiedOnChain,
  });
}
