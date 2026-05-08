import '../../data/trade_order_models.dart';

abstract class TransporterRepository {
  Future<List<TradeOrderDto>> getTransporterOrders();
  Future<TradeOrderDto> getOrderDetails(String orderId);
  Future<void> confirmPickedUp(String orderId);
  Future<void> confirmDelivered(String orderId);
  Future<String> resolveUserLabel(String userId);
  Future<String> resolveBatchName(String batchId);
  Future<String> resolveProductName(String productId);
}
