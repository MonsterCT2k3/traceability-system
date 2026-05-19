import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../domain/usecases/get_trace_by_serial.dart';
import '../../../domain/usecases/verify_trace_on_chain.dart';
import 'trace_event.dart';
import 'trace_state.dart';

class TraceBloc extends Bloc<TraceEvent, TraceState> {
  final GetTraceBySerialUseCase getTraceBySerial;
  final VerifyTraceOnChainUseCase verifyTraceOnChain;

  TraceBloc({
    required this.getTraceBySerial,
    required this.verifyTraceOnChain,
  }) : super(TraceInitial()) {
    
    on<InitializeTraceWithData>((event, emit) {
      emit(TraceLoaded(trace: event.trace));
    });

    on<GetTraceDetails>((event, emit) async {
      emit(TraceLoading());
      final result = await getTraceBySerial.callWithHistory(event.serial, isHistory: event.isHistory);
      result.fold(
        (failure) => emit(TraceError(failure.message)),
        (trace) {
          emit(TraceLoaded(trace: trace));
          add(VerifyTraceBlockchain(event.serial));
        },
      );
    });

    on<VerifyTraceBlockchain>((event, emit) async {
      if (state is! TraceLoaded) return;
      final currentState = state as TraceLoaded;
      
      emit(currentState.copyWith(isVerifying: true));
      
      final result = await verifyTraceOnChain(event.serial);
      
      result.fold(
        (failure) => emit(currentState.copyWith(
          isVerifying: false,
          verificationError: 'Xác thực thất bại: ${failure.message}',
        )),
        (updatedTrace) => emit(TraceLoaded(
          trace: updatedTrace,
          isVerifying: false,
        )),
      );
    });
  }
}
