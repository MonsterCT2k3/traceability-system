import 'package:equatable/equatable.dart';
import '../../../domain/entities/scan_history_entity.dart';

abstract class HistoryState extends Equatable {
  const HistoryState();

  @override
  List<Object?> get props => [];
}

class HistoryInitial extends HistoryState {}

class HistoryLoading extends HistoryState {}

class HistoryLoaded extends HistoryState {
  final List<ScanHistoryEntity> historyList;
  final bool hasReachedMax;

  const HistoryLoaded({
    required this.historyList,
    this.hasReachedMax = false,
  });

  @override
  List<Object?> get props => [historyList, hasReachedMax];
}

class HistoryError extends HistoryState {
  final String message;

  const HistoryError(this.message);

  @override
  List<Object?> get props => [message];
}
