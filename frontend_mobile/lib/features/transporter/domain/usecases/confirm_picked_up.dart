import '../repositories/transporter_repository.dart';

class ConfirmPickedUpUseCase {
  final TransporterRepository repository;

  ConfirmPickedUpUseCase(this.repository);

  Future<void> call(String orderId) {
    return repository.confirmPickedUp(orderId);
  }
}
