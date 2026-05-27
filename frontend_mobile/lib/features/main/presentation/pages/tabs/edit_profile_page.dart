import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:image_picker/image_picker.dart';

import '../../../../../core/widgets/dismiss_keyboard.dart';
import '../../../../auth/presentation/bloc/auth_bloc.dart';
import '../../../../auth/presentation/bloc/auth_event.dart';
import '../../../../auth/presentation/bloc/auth_state.dart';

class EditProfilePage extends StatefulWidget {
  const EditProfilePage({super.key});

  @override
  State<EditProfilePage> createState() => _EditProfilePageState();
}

class _EditProfilePageState extends State<EditProfilePage> {
  final _formKey = GlobalKey<FormState>();
  final _picker = ImagePicker();

  late final TextEditingController _fullNameController;
  late final TextEditingController _emailController;
  late final TextEditingController _phoneController;
  late final TextEditingController _descriptionController;
  late final TextEditingController _locationController;

  File? _imageFile;
  String? _avatarUrl;
  bool _savingProfile = false;
  bool _uploadingAvatar = false;

  @override
  void initState() {
    super.initState();
    final state = context.read<AuthBloc>().state;
    final user = state is AuthAuthenticated ? state.user : null;
    _fullNameController = TextEditingController(text: user?.fullName ?? '');
    _emailController = TextEditingController(text: user?.email ?? '');
    _phoneController = TextEditingController(text: user?.phone ?? '');
    _descriptionController =
        TextEditingController(text: user?.description ?? '');
    _locationController = TextEditingController(text: user?.location ?? '');
    _avatarUrl = user?.avatarUrl;
  }

  @override
  void dispose() {
    _fullNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _descriptionController.dispose();
    _locationController.dispose();
    super.dispose();
  }

  Future<void> _pickImage() async {
    final pickedFile = await _picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 85,
      maxWidth: 1000,
    );
    if (pickedFile == null || !mounted) return;
    final file = File(pickedFile.path);
    setState(() {
      _imageFile = file;
      _uploadingAvatar = true;
    });
    context.read<AuthBloc>().add(UpdateAvatarEvent(imageFile: file));
  }

  void _onSave() {
    FocusScope.of(context).unfocus();
    if (_formKey.currentState?.validate() != true) return;
    setState(() => _savingProfile = true);
    context.read<AuthBloc>().add(
          UpdateProfileEvent(
            fullName: _fullNameController.text.trim(),
            email: _emailController.text.trim(),
            phone: _phoneController.text.trim(),
            description: _descriptionController.text.trim(),
            location: _locationController.text.trim(),
          ),
        );
  }

  InputDecoration _fieldDecoration({
    required String label,
    required IconData icon,
    String? hint,
  }) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      prefixIcon: Icon(icon),
      filled: true,
      fillColor: const Color(0xFFF7FAF8),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: Colors.grey.shade200),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(color: Colors.grey.shade200),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: BorderSide(
          color: Theme.of(context).colorScheme.primary,
          width: 1.5,
        ),
      ),
    );
  }

  String? _validateName(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Vui lòng nhập họ và tên';
    }
    if (value.trim().length < 2) return 'Họ và tên quá ngắn';
    return null;
  }

  String? _validateEmail(String? value) {
    final email = value?.trim() ?? '';
    if (email.isEmpty) return 'Vui lòng nhập email';
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email)) {
      return 'Email không hợp lệ';
    }
    return null;
  }

  String? _validatePhone(String? value) {
    final phone = value?.replaceAll(RegExp(r'\s+'), '') ?? '';
    if (phone.isEmpty) return 'Vui lòng nhập số điện thoại';
    if (!RegExp(r'^[+0-9][0-9]{8,14}$').hasMatch(phone)) {
      return 'Số điện thoại không hợp lệ';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F9F8),
      appBar: AppBar(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        title: const Text(
          'Chỉnh sửa hồ sơ',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
      ),
      body: BlocConsumer<AuthBloc, AuthState>(
        listener: (context, state) {
          if (state is AuthError) {
            setState(() {
              _savingProfile = false;
              _uploadingAvatar = false;
            });
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(state.message),
                backgroundColor: Colors.redAccent,
              ),
            );
            return;
          }
          if (state is AuthAuthenticated) {
            _avatarUrl = state.user.avatarUrl;
            if (_uploadingAvatar) {
              setState(() => _uploadingAvatar = false);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Đã cập nhật ảnh đại diện')),
              );
            }
            if (_savingProfile) {
              setState(() => _savingProfile = false);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Đã lưu thông tin hồ sơ')),
              );
            }
          }
        },
        builder: (context, state) {
          final busy = state is AuthLoading;
          return DismissKeyboardOnTap(
            child: SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _AvatarEditor(
                        imageFile: _imageFile,
                        avatarUrl: _avatarUrl,
                        uploading: _uploadingAvatar && busy,
                        onTap: busy ? null : _pickImage,
                      ),
                      const SizedBox(height: 20),
                      _FormSection(
                        title: 'Thông tin cơ bản',
                        subtitle: 'Thông tin hiển thị cho các bên giao dịch',
                        children: [
                          TextFormField(
                            controller: _fullNameController,
                            enabled: !busy,
                            textInputAction: TextInputAction.next,
                            decoration: _fieldDecoration(
                              label: 'Họ và tên',
                              icon: Icons.person_outline,
                            ),
                            validator: _validateName,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _descriptionController,
                            enabled: !busy,
                            maxLines: 3,
                            decoration: _fieldDecoration(
                              label: 'Giới thiệu',
                              icon: Icons.description_outlined,
                              hint: 'Mô tả ngắn về bạn hoặc đơn vị',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      _FormSection(
                        title: 'Liên hệ',
                        subtitle: 'Dùng để xác nhận và kết nối giao dịch',
                        children: [
                          TextFormField(
                            controller: _emailController,
                            enabled: !busy,
                            keyboardType: TextInputType.emailAddress,
                            textInputAction: TextInputAction.next,
                            decoration: _fieldDecoration(
                              label: 'Email',
                              icon: Icons.email_outlined,
                            ),
                            validator: _validateEmail,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _phoneController,
                            enabled: !busy,
                            keyboardType: TextInputType.phone,
                            textInputAction: TextInputAction.next,
                            decoration: _fieldDecoration(
                              label: 'Số điện thoại',
                              icon: Icons.phone_outlined,
                            ),
                            validator: _validatePhone,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _locationController,
                            enabled: !busy,
                            decoration: _fieldDecoration(
                              label: 'Địa chỉ',
                              icon: Icons.location_on_outlined,
                              hint: 'Khu vực hoạt động',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 22),
                      FilledButton.icon(
                        style: FilledButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 15),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(15),
                          ),
                        ),
                        onPressed: busy ? null : _onSave,
                        icon: busy && _savingProfile
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                  color: Colors.white,
                                  strokeWidth: 2,
                                ),
                              )
                            : const Icon(Icons.save_outlined),
                        label: Text(
                          busy && _savingProfile
                              ? 'Đang lưu...'
                              : 'Lưu thay đổi',
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

class _AvatarEditor extends StatelessWidget {
  const _AvatarEditor({
    required this.imageFile,
    required this.avatarUrl,
    required this.uploading,
    required this.onTap,
  });

  final File? imageFile;
  final String? avatarUrl;
  final bool uploading;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final hasRemote = avatarUrl != null && avatarUrl!.trim().isNotEmpty;
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 22),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF147D52), Color(0xFF27A86B)],
        ),
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        children: [
          Stack(
            children: [
              CircleAvatar(
                radius: 57,
                backgroundColor: Colors.white.withOpacity(0.24),
                child: CircleAvatar(
                  radius: 53,
                  backgroundColor: Colors.white.withOpacity(0.18),
                  backgroundImage: imageFile != null
                      ? FileImage(imageFile!)
                      : (hasRemote ? NetworkImage(avatarUrl!) : null)
                          as ImageProvider?,
                  child: imageFile == null && !hasRemote
                      ? const Icon(
                          Icons.person_rounded,
                          size: 52,
                          color: Colors.white,
                        )
                      : null,
                ),
              ),
              if (uploading)
                const Positioned.fill(
                  child: Center(
                    child: CircularProgressIndicator(color: Colors.white),
                  ),
                ),
              Positioned(
                bottom: 2,
                right: 2,
                child: Material(
                  color: Colors.white,
                  shape: const CircleBorder(),
                  child: IconButton(
                    onPressed: onTap,
                    icon: Icon(
                      Icons.photo_camera_outlined,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          const Text(
            'Ảnh đại diện',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 4),
          const Text(
            'Nhấn biểu tượng máy ảnh để cập nhật',
            style: TextStyle(color: Colors.white70, fontSize: 12),
          ),
        ],
      ),
    );
  }
}

class _FormSection extends StatelessWidget {
  const _FormSection({
    required this.title,
    required this.subtitle,
    required this.children,
  });

  final String title;
  final String subtitle;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 3),
          Text(
            subtitle,
            style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
          ),
          const SizedBox(height: 14),
          ...children,
        ],
      ),
    );
  }
}
