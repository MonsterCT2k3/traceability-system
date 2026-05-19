import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../../../../core/usecases/usecase.dart';
import '../entities/scan_history_entity.dart';
import '../repositories/scan_history_repository.dart';

class GetScanHistoryUseCase implements UseCase<List<ScanHistoryEntity>, GetScanHistoryParams> {
  final ScanHistoryRepository repository;

  GetScanHistoryUseCase(this.repository);

  @override
  Future<Either<Failure, List<ScanHistoryEntity>>> call(GetScanHistoryParams params) async {
    return await repository.getScanHistory(params.page, params.size);
  }
}

class GetScanHistoryParams {
  final int page;
  final int size;

  GetScanHistoryParams({required this.page, required this.size});
}
