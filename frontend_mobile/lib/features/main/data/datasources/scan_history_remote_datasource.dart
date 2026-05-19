import '../../../../core/network/api_client.dart';
import '../models/scan_history_model.dart';

abstract class ScanHistoryRemoteDataSource {
  Future<void> recordScan(String unitSerial);
  Future<List<ScanHistoryModel>> getScanHistory(int page, int size);
}

class ScanHistoryRemoteDataSourceImpl implements ScanHistoryRemoteDataSource {
  final ApiClient apiClient;

  ScanHistoryRemoteDataSourceImpl({required this.apiClient});

  @override
  Future<void> recordScan(String unitSerial) async {
    await apiClient.post(
      '/product/api/v1/scan-history',
      data: {'unitSerial': unitSerial},
    );
  }

  @override
  Future<List<ScanHistoryModel>> getScanHistory(int page, int size) async {
    final response = await apiClient.get(
      '/product/api/v1/scan-history',
      queryParameters: {'page': page, 'size': size},
    );
    final data = response.data['result']['content'] as List;
    return data.map((json) => ScanHistoryModel.fromJson(json)).toList();
  }
}
