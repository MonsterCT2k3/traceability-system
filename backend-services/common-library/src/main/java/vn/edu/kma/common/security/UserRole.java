package vn.edu.kma.common.security;

/**
 * Vai trò hệ thống (một user một role). Giá trị lưu DB / JWT claim {@code role} dùng {@link #name()}.
 */
public enum UserRole {
    ADMIN,
    USER,
    SUPPLIER,
    MANUFACTURER,
    RETAILER,
    TRANSPORTER;

    public String springAuthority() {
        return "ROLE_" + name();
    }

    /**
     * Parse role từ JWT/DB; chuỗi lạ → {@link #USER} để tránh crash (admin đổi role dùng API có validate riêng).
     */
    public static UserRole fromClaimOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            return USER;
        }
        try {
            return UserRole.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }

    /** Dùng khi gán role từ API admin: bắt buộc hợp lệ. */
    public static UserRole parseRequired(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("role không được để trống");
        }
        return UserRole.valueOf(raw.trim().toUpperCase());
    }
}
