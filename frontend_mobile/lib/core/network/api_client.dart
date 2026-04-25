import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ApiClient {
  final Dio dio;
  final SharedPreferences sharedPreferences;

  ApiClient({required this.dio, required this.sharedPreferences}) {
    dio.options.baseUrl = 'https://fbf1-2405-4802-49c-e3d0-c6b-49c3-9647-84e8.ngrok-free.app'; // API Gateway URL
    // dio.options.baseUrl = 'http://localhost:8080'; // API Gateway URL
    dio.options.connectTimeout = const Duration(seconds: 10);
    dio.options.receiveTimeout = const Duration(seconds: 10);

    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        final token = sharedPreferences.getString('accessToken');
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onError: (DioException e, handler) {
        // Handle global errors here like 401 Unauthorized
        return handler.next(e);
      }
    ));
  }

  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) async {
    return await dio.get(path, queryParameters: queryParameters);
  }

  Future<Response> post(String path, {dynamic data}) async {
    return await dio.post(path, data: data);
  }

  // Add PUT, DELETE, etc.
}