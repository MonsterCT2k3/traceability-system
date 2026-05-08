import '../repositories/transporter_repository.dart';
import '../../data/trade_order_models.dart';

class GetOrderDetailsUseCase {
  final TransporterRepository repository;

  GetOrderDetailsUseCase(this.repository);

  Future<TradeOrderDto> call(String orderId) {
    return repository.getOrderDetails(orderId);
  }
}
