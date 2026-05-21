import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  final Dio dio;
  final FlutterSecureStorage secureStorage;

  ApiClient({required this.dio, required this.secureStorage}) {
    dio.options.baseUrl = 'https://bb8d-2401-d800-df80-137f-c43e-3442-ba45-a4c.ngrok-free.app';
    dio.options.connectTimeout = const Duration(seconds: 10);
    dio.options.receiveTimeout = const Duration(seconds: 10);

    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await secureStorage.read(key: 'accessToken');
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onError: (DioException e, handler) async {
        if (e.response?.statusCode == 401) {
          // Attempt to refresh token
          final refreshToken = await secureStorage.read(key: 'refreshToken');
          if (refreshToken != null) {
            try {
              // Create a new Dio instance to avoid infinite interceptor loops
              final tokenDio = Dio(BaseOptions(baseUrl: dio.options.baseUrl));
              final refreshResponse = await tokenDio.post(
                '/identity/api/v1/auth/refresh',
                data: {'refreshToken': refreshToken},
              );

              if (refreshResponse.statusCode == 200) {
                final newAccessToken = refreshResponse.data['result']['accessToken'];
                final newRefreshToken = refreshResponse.data['result']['refreshToken'];

                await secureStorage.write(key: 'accessToken', value: newAccessToken);
                await secureStorage.write(key: 'refreshToken', value: newRefreshToken);

                // Retry original request with new token
                e.requestOptions.headers['Authorization'] = 'Bearer $newAccessToken';
                final retryResponse = await tokenDio.fetch(e.requestOptions);
                return handler.resolve(retryResponse);
              }
            } catch (refreshError) {
              // Refresh failed (e.g., refresh token expired)
              await secureStorage.delete(key: 'accessToken');
              await secureStorage.delete(key: 'refreshToken');
            }
          } else {
            await secureStorage.delete(key: 'accessToken');
            await secureStorage.delete(key: 'refreshToken');
          }
        }
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
}