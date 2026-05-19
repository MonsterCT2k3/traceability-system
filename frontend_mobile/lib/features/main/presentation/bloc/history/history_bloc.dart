import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/usecases/get_scan_history_usecase.dart';
import '../../../domain/usecases/record_scan_history_usecase.dart';
import 'history_event.dart';
import 'history_state.dart';

class HistoryBloc extends Bloc<HistoryEvent, HistoryState> {
  final GetScanHistoryUseCase getScanHistory;
  final RecordScanHistoryUseCase recordScanHistory;

  HistoryBloc({
    required this.getScanHistory,
    required this.recordScanHistory,
  }) : super(HistoryInitial()) {
    on<LoadHistoryEvent>(_onLoadHistory);
    on<RecordHistoryEvent>(_onRecordHistory);
  }

  Future<void> _onLoadHistory(LoadHistoryEvent event, Emitter<HistoryState> emit) async {
    if (event.page == 0) {
      emit(HistoryLoading());
    }
    
    final result = await getScanHistory(GetScanHistoryParams(page: event.page, size: event.size));
    
    result.fold(
      (failure) {
        if (state is! HistoryLoaded) {
          emit(HistoryError(failure.message));
        }
      },
      (historyList) {
        if (event.page == 0) {
          emit(HistoryLoaded(historyList: historyList, hasReachedMax: historyList.length < event.size));
        } else if (state is HistoryLoaded) {
          final currentState = state as HistoryLoaded;
          emit(HistoryLoaded(
            historyList: currentState.historyList + historyList,
            hasReachedMax: historyList.length < event.size,
          ));
        }
      },
    );
  }

  Future<void> _onRecordHistory(RecordHistoryEvent event, Emitter<HistoryState> emit) async {
    // Record silently, do not emit loading/error states that would disrupt the UI
    await recordScanHistory(event.unitSerial);
    // After recording, reload the history so the tab is updated
    add(const LoadHistoryEvent(page: 0));
  }
}
