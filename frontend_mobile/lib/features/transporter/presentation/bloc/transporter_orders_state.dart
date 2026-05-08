import '../../data/trade_order_models.dart';

abstract class TransporterOrdersState {}

class TransporterOrdersInitial extends TransporterOrdersState {}

class TransporterOrdersLoading extends TransporterOrdersState {}

class TransporterOrdersLoaded extends TransporterOrdersState {
  final List<TradeOrderDto> allOrders;
  final List<TradeOrderDto> displayOrders;
  final bool isLoadingMore;

  TransporterOrdersLoaded({
    required this.allOrders,
    required this.displayOrders,
    this.isLoadingMore = false,
  });

  TransporterOrdersLoaded copyWith({
    List<TradeOrderDto>? allOrders,
    List<TradeOrderDto>? displayOrders,
    bool? isLoadingMore,
  }) {
    return TransporterOrdersLoaded(
      allOrders: allOrders ?? this.allOrders,
      displayOrders: displayOrders ?? this.displayOrders,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
    );
  }
}

class TransporterOrdersError extends TransporterOrdersState {
  final String message;

  TransporterOrdersError(this.message);
}
