package vn.edu.kma.product_service.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Khớp {@code PublicRouteMatcher} trên gateway (servlet path trực tiếp tới product: {@code /api/v1/...}).
 */
public final class ProductPublicPathMatcher {

    private ProductPublicPathMatcher() {
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
        return path.matches("/api/v1/products/?$")
                || path.matches("/api/v1/products/[^/]+$")
                || path.matches("/api/v1/products/[^/]+/qr$")
                || path.matches("/api/v1/units/[^/]+/trace$")
                || path.matches("/api/v1/units/[^/]+/qr$")
                || path.matches("/api/v1/units/trace/by-serial$")
                || path.contains("/api/v1/histories/product/")
                || path.matches("/api/v1/blockchain/batch/[^/]+(/exists)?$")
                || path.matches("/api/v1/blockchain/transformed-batch/[^/]+(/exists)?$");
    }

    private static boolean isPublicPost(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return path.matches("/api/v1/units/[^/]+/secret-scan$");
    }
}
