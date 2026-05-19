import 'package:equatable/equatable.dart';

abstract class HistoryEvent extends Equatable {
  const HistoryEvent();

  @override
  List<Object> get props => [];
}

class LoadHistoryEvent extends HistoryEvent {
  final int page;
  final int size;

  const LoadHistoryEvent({this.page = 0, this.size = 20});

  @override
  List<Object> get props => [page, size];
}

class RecordHistoryEvent extends HistoryEvent {
  final String unitSerial;

  const RecordHistoryEvent(this.unitSerial);

  @override
  List<Object> get props => [unitSerial];
}
