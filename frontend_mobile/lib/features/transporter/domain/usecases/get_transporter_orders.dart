import '../repositories/transporter_repository.dart';
import '../../data/trade_order_models.dart';

class GetTransporterOrdersUseCase {
  final TransporterRepository repository;

  GetTransporterOrdersUseCase(this.repository);

  Future<List<TradeOrderDto>> call() {
    return repository.getTransporterOrders();
  }
}
