import '../repositories/transporter_repository.dart';

class ResolveProductNameUseCase {
  final TransporterRepository repository;

  ResolveProductNameUseCase(this.repository);

  Future<String> call(String productId) {
    return repository.resolveProductName(productId);
  }
}
