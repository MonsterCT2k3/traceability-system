/// Đơn xin nâng cấp vai trò (khớp RoleRequestResponse từ identity-service).
class RoleRequest {
  final String id;
  final String requestedRole;
  final String? description;
  final String status;
  final String? createdAt;
  final String? updatedAt;

  const RoleRequest({
    required this.id,
    required this.requestedRole,
    this.description,
    required this.status,
    this.createdAt,
    this.updatedAt,
  });

  factory RoleRequest.fromJson(Map<String, dynamic> json) {
    return RoleRequest(
      id: json['id']?.toString() ?? '',
      requestedRole: json['requestedRole'] ?? '',
      description: json['description'] as String?,
      status: json['status'] ?? '',
      createdAt: json['createdAt']?.toString(),
      updatedAt: json['updatedAt']?.toString(),
    );
  }
}
