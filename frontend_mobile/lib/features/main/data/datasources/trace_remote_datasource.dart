import '../../../../core/network/api_client.dart';
import '../models/trace_model.dart';

abstract class TraceRemoteDataSource {
  Future<TraceModel> getTraceBySerial(String serial);
  Future<TraceModel> getTraceByUnitId(String unitId);
  Future<TraceModel> verifyTraceOnChain(String serial);
}

class TraceRemoteDataSourceImpl implements TraceRemoteDataSource {
  final ApiClient apiClient;

  TraceRemoteDataSourceImpl({required this.apiClient});

  @override
  Future<TraceModel> getTraceBySerial(String serial) async {
    final response = await apiClient.get(
      '/product/api/v1/units/trace/by-serial',
      queryParameters: {'serial': serial},
    );
    return TraceModel.fromJson(response.data['result']);
  }

  @override
  Future<TraceModel> getTraceByUnitId(String unitId) async {
    final response = await apiClient.get('/product/api/v1/units/$unitId/trace');
    return TraceModel.fromJson(response.data['result']);
  }

  @override
  Future<TraceModel> verifyTraceOnChain(String serial) async {
    final response = await apiClient.get(
      '/product/api/v1/units/trace/by-serial/verify',
      queryParameters: {'serial': serial},
    );
    return TraceModel.fromJson(response.data['result']);
  }
}
