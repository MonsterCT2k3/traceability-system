import 'dart:io';
import 'package:equatable/equatable.dart';

abstract class AuthEvent extends Equatable {
  const AuthEvent();

  @override
  List<Object> get props => [];
}

class LoginEvent extends AuthEvent {
  final String username;
  final String password;

  const LoginEvent({required this.username, required this.password});

  @override
  List<Object> get props => [username, password];
}

class LogoutEvent extends AuthEvent {}

class UpdateProfileEvent extends AuthEvent {
  final String fullName;
  final String email;
  final String phone;
  final String description;
  final String location;

  const UpdateProfileEvent({
    required this.fullName,
    required this.email,
    required this.phone,
    required this.description,
    required this.location,
  });

  @override
  List<Object> get props => [fullName, email, phone, description, location];
}

class UpdateAvatarEvent extends AuthEvent {
  final File imageFile;

  const UpdateAvatarEvent({required this.imageFile});

  @override
  List<Object> get props => [imageFile];
}
