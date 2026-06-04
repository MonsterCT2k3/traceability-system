import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../entities/scan_history_entity.dart';

abstract class ScanHistoryRepository {
  Future<Either<Failure, void>> recordScan(String unitSerial);
  Future<Either<Failure, List<ScanHistoryEntity>>> getScanHistory(
      int page, int size);
}
