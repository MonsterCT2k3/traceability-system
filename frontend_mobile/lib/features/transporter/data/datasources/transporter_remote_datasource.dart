import '../../../../core/network/api_client.dart';
import '../../data/trade_order_models.dart';

abstract class TransporterRemoteDataSource {
  Future<List<TradeOrderDto>> getTransporterOrders();
  Future<TradeOrderDto> getOrderDetails(String orderId);
  Future<void> confirmPickedUp(String orderId);
  Future<void> confirmDelivered(String orderId);
  Future<Map<String, dynamic>?> getUserDirectory(String userId);
  Future<Map<String, dynamic>?> getRawBatchDetail(String batchId);
  Future<Map<String, dynamic>?> getProductDetail(String productId);
}

class TransporterRemoteDataSourceImpl implements TransporterRemoteDataSource {
  final ApiClient apiClient;

  TransporterRemoteDataSourceImpl({required this.apiClient});

  @override
  Future<List<TradeOrderDto>> getTransporterOrders() async {
    final response = await apiClient.get('/trade/api/v1/orders/mine/carrier');
    final raw = response.data;
    if (raw is Map && raw['result'] is List) {
      return (raw['result'] as List)
          .map((e) => TradeOrderDto.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    }
    return [];
  }

  @override
  Future<TradeOrderDto> getOrderDetails(String orderId) async {
    final response = await apiClient.get('/trade/api/v1/orders/$orderId');
    final raw = response.data;
    if (raw is Map && raw['result'] != null) {
      return TradeOrderDto.fromJson(Map<String, dynamic>.from(raw['result'] as Map));
    }
    throw Exception('Failed to get order details');
  }

  @override
  Future<void> confirmPickedUp(String orderId) async {
    await apiClient.post('/trade/api/v1/orders/$orderId/confirm-picked-up');
  }

  @override
  Future<void> confirmDelivered(String orderId) async {
    await apiClient.post('/trade/api/v1/orders/$orderId/confirm-delivered');
  }

  @override
  Future<Map<String, dynamic>?> getUserDirectory(String userId) async {
    try {
      final response = await apiClient.get('/identity/api/v1/users/directory/by-id/${Uri.encodeComponent(userId)}');
      final raw = response.data;
      if (raw is Map && raw['result'] != null) {
        return Map<String, dynamic>.from(raw['result'] as Map);
      }
    } catch (_) {}
    return null;
  }

  @override
  Future<Map<String, dynamic>?> getRawBatchDetail(String batchId) async {
    try {
      final response = await apiClient.get('/product/api/v1/raw-batches/$batchId');
      final raw = response.data;
      if (raw is Map && raw['result'] != null) {
        return Map<String, dynamic>.from(raw['result'] as Map);
      }
    } catch (_) {}
    return null;
  }

  @override
  Future<Map<String, dynamic>?> getProductDetail(String productId) async {
    try {
      final response = await apiClient.get('/catalog/api/v1/products/$productId');
      final raw = response.data;
      if (raw is Map && raw['result'] != null) {
        return Map<String, dynamic>.from(raw['result'] as Map);
      }
    } catch (_) {}
    return null;
  }
}
