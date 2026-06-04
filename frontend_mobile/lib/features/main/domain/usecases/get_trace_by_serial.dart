import 'package:dartz/dartz.dart';
import '../../../../core/error/failures.dart';
import '../../../../core/usecases/usecase.dart';
import '../entities/trace_entity.dart';
import '../repositories/trace_repository.dart';

class GetTraceBySerialUseCase implements UseCase<TraceEntity, String> {
  final TraceRepository repository;

  GetTraceBySerialUseCase(this.repository);

  @override
  Future<Either<Failure, TraceEntity>> call(String serial) async {
    return await repository.getTraceBySerial(serial);
  }

  Future<Either<Failure, TraceEntity>> callWithHistory(String serial,
      {bool isHistory = false}) async {
    return await repository.getTraceBySerial(serial, isHistory: isHistory);
  }
}
