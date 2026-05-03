import 'package:equatable/equatable.dart';
import '../../../domain/entities/trace_entity.dart';

abstract class TraceState extends Equatable {
  const TraceState();
  
  @override
  List<Object?> get props => [];
}

class TraceInitial extends TraceState {}

class TraceLoading extends TraceState {}

class TraceLoaded extends TraceState {
  final TraceEntity trace;
  final bool isVerifying;
  final String? verificationError;

  const TraceLoaded({
    required this.trace,
    this.isVerifying = false,
    this.verificationError,
  });

  TraceLoaded copyWith({
    TraceEntity? trace,
    bool? isVerifying,
    String? verificationError,
  }) {
    return TraceLoaded(
      trace: trace ?? this.trace,
      isVerifying: isVerifying ?? this.isVerifying,
      verificationError: verificationError, // Cho phép reset error thành null
    );
  }

  @override
  List<Object?> get props => [trace, isVerifying, verificationError];
}

class TraceError extends TraceState {
  final String message;
  const TraceError(this.message);

  @override
  List<Object?> get props => [message];
}
