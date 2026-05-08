import '../repositories/transporter_repository.dart';

class ResolveBatchNameUseCase {
  final TransporterRepository repository;

  ResolveBatchNameUseCase(this.repository);

  Future<String> call(String batchId) {
    return repository.resolveBatchName(batchId);
  }
}
