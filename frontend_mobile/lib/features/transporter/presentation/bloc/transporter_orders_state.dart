import '../../data/trade_order_models.dart';

abstract class TransporterOrdersState {}

class TransporterOrdersInitial extends TransporterOrdersState {}

class TransporterOrdersLoading extends TransporterOrdersState {}

class TransporterOrdersLoaded extends TransporterOrdersState {
  final List<TradeOrderDto> allOrders;
  final List<TradeOrderDto> displayOrders;
  final bool isLoadingMore;
  final bool isRefreshing;

  TransporterOrdersLoaded({
    required this.allOrders,
    required this.displayOrders,
    this.isLoadingMore = false,
    this.isRefreshing = false,
  });

  TransporterOrdersLoaded copyWith({
    List<TradeOrderDto>? allOrders,
    List<TradeOrderDto>? displayOrders,
    bool? isLoadingMore,
    bool? isRefreshing,
  }) {
    return TransporterOrdersLoaded(
      allOrders: allOrders ?? this.allOrders,
      displayOrders: displayOrders ?? this.displayOrders,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      isRefreshing: isRefreshing ?? this.isRefreshing,
    );
  }
}

class TransporterOrdersError extends TransporterOrdersState {
  final String message;

  TransporterOrdersError(this.message);
}
