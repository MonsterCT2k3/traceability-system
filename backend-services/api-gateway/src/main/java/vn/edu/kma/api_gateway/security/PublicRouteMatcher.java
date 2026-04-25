package vn.edu.kma.api_gateway.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Các request <strong>không</strong> cần {@code Authorization: Bearer} khi đi
 * qua gateway.
 * <p>
 * Khớp prefix service: {@code /identity/...}, {@code /product/...},
 * {@code /blockchain/...}
 * (servlet path thường có dạng {@code /product/api/v1/...}).
 * <ul>
 * <li><b>Auth</b>: mọi method dưới {@code /api/v1/auth/} (login, register,
 * refresh, logout, introspect)</li>
 * <li><b>CORS preflight</b>: {@code OPTIONS} — luôn cho qua</li>
 * <li><b>GET công khai</b>: catalog sản phẩm, trace unit, lịch sử theo product,
 * đọc batch trên chain</li>
 * <li><b>POST công khai</b>: {@code .../units/{id}/secret-scan}</li>
 * </ul>
 * Các API như {@code /api/v1/transfers/**}, tạo sản phẩm, claim unit… vẫn cần
 * JWT.
 */
public final class PublicRouteMatcher {

    private PublicRouteMatcher() {
    }

    public static boolean isPublicUnauthenticated(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (path.contains("/api/v1/auth/")) {
            return true;
        }

        if (isPublicGet(method, path)) {
            return true;
        }

        return isPublicPost(method, path);
    }

    private static boolean isPublicGet(String method, String path) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if (path.matches(".*/actuator/(health|info)$")) {
            return true;
        }
        return path.matches(".*/api/v1/products/?$")
                || path.matches(".*/api/v1/products/[^/]+$")
                || path.matches(".*/api/v1/products/[^/]+/qr$")
                || path.matches(".*/api/v1/material-catalog/?$")
                || path.matches(".*/api/v1/units/[^/]+/trace$")
                || path.matches(".*/api/v1/units/[^/]+/qr$")
                || path.matches(".*/api/v1/units/trace/by-serial$")
                || path.contains("/api/v1/histories/product/")
                || path.matches(".*/api/v1/blockchain/batch/[^/]+(/exists)?$")
                || path.matches(".*/api/v1/blockchain/transformed-batch/[^/]+(/exists)?$");
    }

    private static boolean isPublicPost(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return path.matches(".*/api/v1/units/[^/]+/secret-scan$");
    }
}
