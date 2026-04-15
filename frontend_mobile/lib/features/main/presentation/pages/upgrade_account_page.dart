import 'package:flutter/material.dart';
import '../../../profile/domain/entities/role_request.dart';
import '../../../../injection_container.dart';
import '../../../auth/domain/repositories/auth_repository.dart';
import '../../../../core/widgets/dismiss_keyboard.dart';

/// Vai trò có thể xin qua API (khớp [UserRole] backend, không gồm ADMIN/USER).
const Map<String, String> _upgradeRoleLabels = {
  'SUPPLIER': 'Nhà cung cấp (Supplier)',
  'MANUFACTURER': 'Nhà sản xuất (Manufacturer)',
  'RETAILER': 'Nhà bán lẻ (Retailer)',
  'TRANSPORTER': 'Đơn vị vận chuyển (Transporter)',
};

class UpgradeAccountPage extends StatefulWidget {
  const UpgradeAccountPage({super.key});

  @override
  State<UpgradeAccountPage> createState() => _UpgradeAccountPageState();
}

class _UpgradeAccountPageState extends State<UpgradeAccountPage> {
  final _formKey = GlobalKey<FormState>();
  final _descriptionController = TextEditingController();
  String? _selectedRole;
  List<RoleRequest> _requests = [];
  bool _loading = true;
  bool _submitting = false;
  String? _error;

  AuthRepository get _repo => sl<AuthRepository>();

  @override
  void initState() {
    super.initState();
    _loadRequests();
  }

  @override
  void dispose() {
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _loadRequests() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final result = await _repo.getMyRoleRequests();
    if (!mounted) return;
    result.fold(
      (f) => setState(() {
        _loading = false;
        _error = f.message;
      }),
      (list) => setState(() {
        _loading = false;
        _requests = list;
      }),
    );
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;
    if (_selectedRole == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn vai trò muốn nâng cấp'), backgroundColor: Colors.orange),
      );
      return;
    }

    setState(() => _submitting = true);
    final result = await _repo.createRoleRequest(_selectedRole!, _descriptionController.text.trim());
    if (!mounted) return;
    setState(() => _submitting = false);

    result.fold(
      (f) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(f.message), backgroundColor: Colors.redAccent),
        );
      },
      (_) {
        _descriptionController.clear();
        setState(() => _selectedRole = null);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã gửi đơn thành công!'), backgroundColor: Colors.green),
        );
        _loadRequests();
      },
    );
  }

  String _statusLabel(String status) {
    switch (status) {
      case 'PENDING':
        return 'Chờ duyệt';
      case 'APPROVED':
        return 'Đã duyệt';
      case 'REJECTED':
        return 'Từ chối';
      default:
        return status;
    }
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'PENDING':
        return Colors.orange;
      case 'APPROVED':
        return Colors.green;
      case 'REJECTED':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: Colors.black87),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Nâng cấp tài khoản',
          style: TextStyle(color: Colors.black87, fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
      ),
      body: DismissKeyboardOnTap(
        child: SafeArea(
          child: RefreshIndicator(
            onRefresh: _loadRequests,
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(20),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      'Gửi đơn đăng ký vai trò mới. Admin sẽ xem xét và phản hồi.',
                      style: TextStyle(fontSize: 14, color: Colors.grey.shade700, height: 1.4),
                    ),
                    const SizedBox(height: 20),
                    DropdownButtonFormField<String>(
                      value: _selectedRole,
                      decoration: InputDecoration(
                        labelText: 'Vai trò muốn đăng ký',
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                        prefixIcon: const Icon(Icons.badge_outlined),
                      ),
                      items: _upgradeRoleLabels.entries
                          .map(
                            (e) => DropdownMenuItem(value: e.key, child: Text(e.value)),
                          )
                          .toList(),
                      onChanged: _submitting
                          ? null
                          : (v) => setState(() => _selectedRole = v),
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _descriptionController,
                      maxLines: 4,
                      decoration: InputDecoration(
                        labelText: 'Mô tả / lý do',
                        hintText: 'Giới thiệu ngắn về doanh nghiệp hoặc nhu cầu sử dụng hệ thống...',
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                        alignLabelWithHint: true,
                      ),
                      validator: (v) {
                        if (v == null || v.trim().isEmpty) return 'Vui lòng nhập mô tả';
                        return null;
                      },
                      enabled: !_submitting,
                    ),
                    const SizedBox(height: 20),
                    SizedBox(
                      height: 48,
                      child: ElevatedButton(
                        onPressed: _submitting ? null : _submit,
                        style: ElevatedButton.styleFrom(
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        child: _submitting
                            ? const SizedBox(
                                width: 22,
                                height: 22,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                              )
                            : const Text('Gửi đơn', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      ),
                    ),
                    const SizedBox(height: 32),
                    Row(
                      children: [
                        Icon(Icons.history, size: 22, color: theme.colorScheme.primary),
                        const SizedBox(width: 8),
                        Text(
                          'Đơn đã gửi',
                          style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    if (_loading)
                      const Padding(
                        padding: EdgeInsets.all(24),
                        child: Center(child: CircularProgressIndicator()),
                      )
                    else if (_error != null)
                      Text(_error!, style: const TextStyle(color: Colors.redAccent))
                    else if (_requests.isEmpty)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        child: Text(
                          'Chưa có đơn nào.',
                          style: TextStyle(color: Colors.grey.shade600),
                        ),
                      )
                    else
                      ..._requests.map((r) {
                        final roleLabel = _upgradeRoleLabels[r.requestedRole] ?? r.requestedRole;
                        return Card(
                          margin: const EdgeInsets.only(bottom: 10),
                          elevation: 0,
                          color: Colors.grey.shade50,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          child: Padding(
                            padding: const EdgeInsets.all(14),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    Expanded(
                                      child: Text(
                                        roleLabel,
                                        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                                      ),
                                    ),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                      decoration: BoxDecoration(
                                        color: _statusColor(r.status).withOpacity(0.15),
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Text(
                                        _statusLabel(r.status),
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color: _statusColor(r.status),
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                                if (r.description != null && r.description!.isNotEmpty) ...[
                                  const SizedBox(height: 8),
                                  Text(r.description!, style: TextStyle(fontSize: 13, color: Colors.grey.shade800)),
                                ],
                                if (r.createdAt != null) ...[
                                  const SizedBox(height: 8),
                                  Text(
                                    'Gửi lúc: ${r.createdAt}',
                                    style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                                  ),
                                ],
                              ],
                            ),
                          ),
                        );
                      }),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
