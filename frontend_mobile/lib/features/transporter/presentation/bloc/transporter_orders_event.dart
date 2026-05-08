import '../../data/trade_order_models.dart';

abstract class TransporterOrdersEvent {}

class FetchTransporterOrdersEvent extends TransporterOrdersEvent {}

class LoadMoreOrdersEvent extends TransporterOrdersEvent {}

class ConfirmPickedUpEvent extends TransporterOrdersEvent {
  final TradeOrderDto order;
  ConfirmPickedUpEvent(this.order);
}

class ConfirmDeliveredEvent extends TransporterOrdersEvent {
  final TradeOrderDto order;
  ConfirmDeliveredEvent(this.order);
}
