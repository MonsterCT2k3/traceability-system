import '../../domain/repositories/transporter_repository.dart';
import '../datasources/transporter_remote_datasource.dart';
import '../../data/trade_order_models.dart';

class TransporterRepositoryImpl implements TransporterRepository {
  final TransporterRemoteDataSource remoteDataSource;

  TransporterRepositoryImpl({required this.remoteDataSource});

  @override
  Future<List<TradeOrderDto>> getTransporterOrders() {
    return remoteDataSource.getTransporterOrders();
  }

  @override
  Future<TradeOrderDto> getOrderDetails(String orderId) {
    return remoteDataSource.getOrderDetails(orderId);
  }

  @override
  Future<void> confirmPickedUp(String orderId) {
    return remoteDataSource.confirmPickedUp(orderId);
  }

  @override
  Future<void> confirmDelivered(String orderId) {
    return remoteDataSource.confirmDelivered(orderId);
  }

  @override
  Future<String> resolveUserLabel(String userId) async {
    if (userId.isEmpty) return '—';
    final u = await remoteDataSource.getUserDirectory(userId);
    if (u == null) return userId;

    final fullName = u['fullName']?.toString().trim() ?? '';
    final username = u['username']?.toString().trim() ?? '';
    final name =
        fullName.isNotEmpty ? fullName : (username.isNotEmpty ? username : '');
    final id = u['id']?.toString() ?? userId;
    if (name.isNotEmpty) return '$name ($id)';
    return '($id)';
  }

  @override
  Future<String> resolveBatchName(String batchId) async {
    if (batchId.isEmpty) return '—';
    final batch = await remoteDataSource.getRawBatchDetail(batchId);
    if (batch == null) return batchId;
    return batch['materialName']?.toString() ?? batchId;
  }

  @override
  Future<String> resolveProductName(String productId) async {
    if (productId.isEmpty) return '—';
    final product = await remoteDataSource.getProductDetail(productId);
    if (product == null) return productId;
    return product['name']?.toString() ?? productId;
  }
}
