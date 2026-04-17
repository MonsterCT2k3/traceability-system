/** Vai trò được phép dùng phiên bản web (có màn hình tương ứng). */
export const WEB_ALLOWED_ROLES = ['ADMIN', 'USER', 'SUPPLIER', 'MANUFACTURER'];

/** Vai trò chỉ dùng app mobile — trên web hiển thị hướng dẫn sang mobile. */
export const MOBILE_ONLY_ROLES = ['RETAILER', 'TRANSPORTER'];

export function normalizeRole(role) {
  return (role ?? '').toString().trim().toUpperCase();
}

export function isWebAllowedRole(role) {
  return WEB_ALLOWED_ROLES.includes(normalizeRole(role));
}
