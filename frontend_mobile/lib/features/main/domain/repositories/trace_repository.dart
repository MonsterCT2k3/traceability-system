import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../entities/trace_entity.dart';

abstract class TraceRepository {
  Future<Either<Failure, TraceEntity>> getTraceBySerial(String serial);
  Future<Either<Failure, TraceEntity>> getTraceByUnitId(String unitId);
  Future<Either<Failure, TraceEntity>> verifyTraceOnChain(String serial);
}
