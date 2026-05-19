import 'package:equatable/equatable.dart';
import '../../../domain/entities/trace_entity.dart';

abstract class TraceEvent extends Equatable {
  const TraceEvent();

  @override
  List<Object?> get props => [];
}

class GetTraceDetails extends TraceEvent {
  final String serial;
  final bool isHistory;
  const GetTraceDetails(this.serial, {this.isHistory = false});

  @override
  List<Object?> get props => [serial, isHistory];
}

class VerifyTraceBlockchain extends TraceEvent {
  final String serial;
  const VerifyTraceBlockchain(this.serial);

  @override
  List<Object?> get props => [serial];
}

class InitializeTraceWithData extends TraceEvent {
  final TraceEntity trace;
  const InitializeTraceWithData(this.trace);

  @override
  List<Object?> get props => [trace];
}
