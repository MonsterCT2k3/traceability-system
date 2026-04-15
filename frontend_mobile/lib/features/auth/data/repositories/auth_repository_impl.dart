import 'dart:io';
import 'package:dartz/dartz.dart';
import 'package:dio/dio.dart';
import '../../../../core/error/failures.dart';
import '../../../profile/domain/entities/role_request.dart';
import '../../domain/entities/user.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../../../core/network/api_client.dart';

class AuthRepositoryImpl implements AuthRepository {
  final ApiClient apiClient;

  AuthRepositoryImpl({required this.apiClient});

  @override
  Future<Either<Failure, User>> login(String username, String password) async {
    try {
      // 1. Gọi API Login để lấy Token
      final loginResponse = await apiClient.post('/identity/api/v1/auth/login', data: {
        'username': username,
        'password': password,
      });

      if (loginResponse.statusCode == 200) {
        final data = loginResponse.data['result'];
        final accessToken = data['accessToken'];
        final refreshToken = data['refreshToken'];

        // Lưu token vào SharedPreferences
        await apiClient.sharedPreferences.setString('accessToken', accessToken);
        await apiClient.sharedPreferences.setString('refreshToken', refreshToken);

        // 2. Gọi API lấy thông tin Profile
        final profileResponse = await apiClient.get('/identity/api/v1/users/profile');
        if (profileResponse.statusCode == 200) {
          final userData = profileResponse.data['result'];
          return Right(User.fromJson(userData));
        } else {
          return const Left(ServerFailure('Không thể lấy thông tin người dùng'));
        }
      } else {
        return const Left(ServerFailure('Tài khoản hoặc mật khẩu không chính xác'));
      }
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối đến máy chủ';
      return Left(ServerFailure(message));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, void>> register(String username, String password, String fullName, String email, String phone) async {
    try {
      final response = await apiClient.post('/identity/api/v1/auth/register', data: {
        'username': username,
        'password': password,
        'fullName': fullName,
        'email': email,
        'phone': phone,
      });

      if (response.statusCode == 201 || response.statusCode == 200) {
        return const Right(null);
      } else {
        return const Left(ServerFailure('Đăng ký thất bại'));
      }
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối đến máy chủ';
      return Left(ServerFailure(message));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, User>> updateProfile(String fullName, String email, String phone, String description, String location) async {
    try {
      final response = await apiClient.dio.put('/identity/api/v1/users/profile', data: {
        'fullName': fullName,
        'email': email,
        'phone': phone,
        'description': description,
        'location': location,
      });

      if (response.statusCode == 200) {
        final userData = response.data['result'];
        return Right(User.fromJson(userData));
      } else {
        return const Left(ServerFailure('Cập nhật thông tin thất bại'));
      }
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối đến máy chủ';
      return Left(ServerFailure(message));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, User>> updateAvatar(File imageFile) async {
    try {
      String fileName = imageFile.path.split('/').last;
      FormData formData = FormData.fromMap({
        "file": await MultipartFile.fromFile(imageFile.path, filename: fileName),
      });

      final response = await apiClient.dio.post('/identity/api/v1/users/profile/avatar', data: formData);

      if (response.statusCode == 200) {
        final userData = response.data['result'];
        return Right(User.fromJson(userData));
      } else {
        return const Left(ServerFailure('Cập nhật ảnh đại diện thất bại'));
      }
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối đến máy chủ';
      return Left(ServerFailure(message));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, List<RoleRequest>>> getMyRoleRequests() async {
    try {
      final response = await apiClient.get('/identity/api/v1/users/role-requests');
      if (response.statusCode == 200) {
        final list = response.data['result'];
        if (list is! List) {
          return const Right([]);
        }
        return Right(
          list.map((e) => RoleRequest.fromJson(Map<String, dynamic>.from(e as Map))).toList(),
        );
      }
      return const Left(ServerFailure('Không tải được danh sách đơn'));
    } on DioException catch (e) {
      final message = e.response?.data['message'] ?? 'Lỗi kết nối đến máy chủ';
      return Left(ServerFailure(message is String ? message : message.toString()));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, RoleRequest>> createRoleRequest(String requestedRole, String description) async {
    try {
      final response = await apiClient.post('/identity/api/v1/users/role-requests', data: {
        'requestedRole': requestedRole,
        'description': description,
      });
      if (response.statusCode == 200) {
        final data = response.data['result'];
        return Right(RoleRequest.fromJson(Map<String, dynamic>.from(data as Map)));
      }
      return const Left(ServerFailure('Gửi đơn thất bại'));
    } on DioException catch (e) {
      final raw = e.response?.data;
      String message = 'Lỗi kết nối đến máy chủ';
      if (raw is Map && raw['message'] != null) {
        message = raw['message'].toString();
      }
      return Left(ServerFailure(message));
    } catch (e) {
      return Left(ServerFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, void>> logout() async {
    await apiClient.sharedPreferences.remove('accessToken');
    await apiClient.sharedPreferences.remove('refreshToken');
    return const Right(null);
  }
}
