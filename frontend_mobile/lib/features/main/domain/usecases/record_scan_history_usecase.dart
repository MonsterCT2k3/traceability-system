import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../../../../core/usecases/usecase.dart';
import '../repositories/scan_history_repository.dart';

class RecordScanHistoryUseCase implements UseCase<void, String> {
  final ScanHistoryRepository repository;

  RecordScanHistoryUseCase(this.repository);

  @override
  Future<Either<Failure, void>> call(String unitSerial) async {
    return await repository.recordScan(unitSerial);
  }
}
