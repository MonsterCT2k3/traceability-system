/// Vai trò được phép đăng nhập trên app mobile.
class PlatformRoles {
  PlatformRoles._();

  static const Set<String> mobileAllowed = {
    'USER',
    'RETAILER',
    'TRANSPORTER',
  };

  static String normalize(String? role) => (role ?? '').trim().toUpperCase();

  static bool isMobileAllowed(String? role) =>
      mobileAllowed.contains(normalize(role));
}
