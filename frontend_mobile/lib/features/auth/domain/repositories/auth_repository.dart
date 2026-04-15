import 'package:dartz/dartz.dart';
import 'dart:io';
import '../../../../core/error/failures.dart';
import '../../../profile/domain/entities/role_request.dart';
import '../entities/user.dart';

abstract class AuthRepository {
  Future<Either<Failure, User>> login(String username, String password);
  Future<Either<Failure, void>> register(String username, String password, String fullName, String email, String phone);
  Future<Either<Failure, User>> updateProfile(String fullName, String email, String phone, String description, String location);
  Future<Either<Failure, User>> updateAvatar(File imageFile);
  Future<Either<Failure, List<RoleRequest>>> getMyRoleRequests();
  Future<Either<Failure, RoleRequest>> createRoleRequest(String requestedRole, String description);
  Future<Either<Failure, void>> logout();
}