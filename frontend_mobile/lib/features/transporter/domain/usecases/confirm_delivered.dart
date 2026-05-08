import '../repositories/transporter_repository.dart';

class ConfirmDeliveredUseCase {
  final TransporterRepository repository;

  ConfirmDeliveredUseCase(this.repository);

  Future<void> call(String orderId) {
    return repository.confirmDelivered(orderId);
  }
}
