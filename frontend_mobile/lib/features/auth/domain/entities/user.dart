import 'package:equatable/equatable.dart';

class User extends Equatable {
  final String id;
  final String username;
  final String fullName;
  final String role;
  final String? email;
  final String? phone;
  final String? avatarUrl;
  final String? location;
  final String? description;

  const User({
    required this.id,
    required this.username,
    required this.fullName,
    required this.role,
    this.email,
    this.phone,
    this.avatarUrl,
    this.location,
    this.description,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] ?? '',
      username: json['username'] ?? '',
      fullName: json['fullName'] ?? '',
      role: json['role'] ?? 'USER',
      email: json['email'],
      phone: json['phone'],
      avatarUrl: json['avatarUrl'],
      location: json['location'],
      description: json['description'],
    );
  }

  @override
  List<Object?> get props => [id, username, fullName, role, email, phone, avatarUrl, location, description];
}