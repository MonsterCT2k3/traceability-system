import 'package:dartz/dartz.dart';
import 'package:dio/dio.dart';
import '../../../../core/error/failures.dart';
import '../../domain/entities/trace_entity.dart';
import '../../domain/repositories/trace_repository.dart';
import '../datasources/trace_remote_datasource.dart';

class TraceRepositoryImpl implements TraceRepository {
  final TraceRemoteDataSource remoteDataSource;

  TraceRepositoryImpl({required this.remoteDataSource});

  @override
  Future<Either<Failure, TraceEntity>> getTraceBySerial(String serial,
      {bool isHistory = false}) async {
    try {
      final model =
          await remoteDataSource.getTraceBySerial(serial, isHistory: isHistory);
      return Right(model);
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối máy chủ';
      return Left(ServerFailure(message.toString()));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, TraceEntity>> getTraceByUnitId(String unitId) async {
    try {
      final model = await remoteDataSource.getTraceByUnitId(unitId);
      return Right(model);
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối máy chủ';
      return Left(ServerFailure(message.toString()));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, TraceEntity>> verifyTraceOnChain(String serial) async {
    try {
      final model = await remoteDataSource.verifyTraceOnChain(serial);
      return Right(model);
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối máy chủ';
      return Left(ServerFailure(message.toString()));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }
}
