import '../repositories/transporter_repository.dart';

class ResolveUserLabelUseCase {
  final TransporterRepository repository;

  ResolveUserLabelUseCase(this.repository);

  Future<String> call(String userId) {
    return repository.resolveUserLabel(userId);
  }
}
