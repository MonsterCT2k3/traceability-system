import 'package:dartz/dartz.dart';
import 'package:dio/dio.dart';
import '../../../../core/error/failures.dart';
import '../../domain/entities/scan_history_entity.dart';
import '../../domain/repositories/scan_history_repository.dart';
import '../datasources/scan_history_remote_datasource.dart';

class ScanHistoryRepositoryImpl implements ScanHistoryRepository {
  final ScanHistoryRemoteDataSource remoteDataSource;

  ScanHistoryRepositoryImpl({required this.remoteDataSource});

  @override
  Future<Either<Failure, void>> recordScan(String unitSerial) async {
    try {
      await remoteDataSource.recordScan(unitSerial);
      return const Right(null);
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối máy chủ';
      return Left(ServerFailure(message.toString()));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, List<ScanHistoryEntity>>> getScanHistory(int page, int size) async {
    try {
      final models = await remoteDataSource.getScanHistory(page, size);
      return Right(models);
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối máy chủ';
      return Left(ServerFailure(message.toString()));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }
}
