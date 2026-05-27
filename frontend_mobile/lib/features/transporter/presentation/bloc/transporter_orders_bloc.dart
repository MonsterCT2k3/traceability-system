import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/usecases/get_transporter_orders.dart';
import '../../domain/usecases/confirm_picked_up.dart';
import '../../domain/usecases/confirm_delivered.dart';
import 'transporter_orders_event.dart';
import 'transporter_orders_state.dart';

class TransporterOrdersBloc
    extends Bloc<TransporterOrdersEvent, TransporterOrdersState> {
  final GetTransporterOrdersUseCase getTransporterOrders;
  final ConfirmPickedUpUseCase confirmPickedUp;
  final ConfirmDeliveredUseCase confirmDelivered;

  final int pageSize = 10;
  int _currentPage = 0;

  TransporterOrdersBloc({
    required this.getTransporterOrders,
    required this.confirmPickedUp,
    required this.confirmDelivered,
  }) : super(TransporterOrdersInitial()) {
    on<FetchTransporterOrdersEvent>(_onFetchOrders);
    on<LoadMoreOrdersEvent>(_onLoadMoreOrders);
    on<ConfirmPickedUpEvent>(_onConfirmPickedUp);
    on<ConfirmDeliveredEvent>(_onConfirmDelivered);
  }

  Future<void> _onFetchOrders(FetchTransporterOrdersEvent event,
      Emitter<TransporterOrdersState> emit) async {
    final previous = state is TransporterOrdersLoaded
        ? state as TransporterOrdersLoaded
        : null;
    if (previous == null) {
      emit(TransporterOrdersLoading());
    } else {
      emit(previous.copyWith(isRefreshing: true, isLoadingMore: false));
    }
    try {
      final allOrders = await getTransporterOrders();
      _currentPage = 0;
      final displayOrders = allOrders.take(pageSize).toList();
      emit(TransporterOrdersLoaded(
          allOrders: allOrders, displayOrders: displayOrders));
    } catch (e) {
      emit(TransporterOrdersError(e.toString()));
    }
  }

  Future<void> _onLoadMoreOrders(
      LoadMoreOrdersEvent event, Emitter<TransporterOrdersState> emit) async {
    if (state is TransporterOrdersLoaded) {
      final currentState = state as TransporterOrdersLoaded;
      if (currentState.isLoadingMore ||
          currentState.displayOrders.length >= currentState.allOrders.length) {
        return;
      }

      emit(currentState.copyWith(isLoadingMore: true));

      await Future.delayed(
          const Duration(milliseconds: 300)); // Simulate UI loading effect

      _currentPage++;
      final nextItems = currentState.allOrders
          .skip(_currentPage * pageSize)
          .take(pageSize)
          .toList();
      final updatedDisplayOrders = List.of(currentState.displayOrders)
        ..addAll(nextItems);

      emit(currentState.copyWith(
        displayOrders: updatedDisplayOrders,
        isLoadingMore: false,
      ));
    }
  }

  Future<void> _onConfirmPickedUp(
      ConfirmPickedUpEvent event, Emitter<TransporterOrdersState> emit) async {
    try {
      await confirmPickedUp(event.order.id);
      add(FetchTransporterOrdersEvent());
    } catch (e) {
      // Typically we'd emit an error state that doesn't clear the list,
      // but for simplicity we can just emit an error. A better way is using a side-effect.
      emit(TransporterOrdersError(e.toString()));
    }
  }

  Future<void> _onConfirmDelivered(
      ConfirmDeliveredEvent event, Emitter<TransporterOrdersState> emit) async {
    try {
      await confirmDelivered(event.order.id);
      add(FetchTransporterOrdersEvent());
    } catch (e) {
      emit(TransporterOrdersError(e.toString()));
    }
  }
}
