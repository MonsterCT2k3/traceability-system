package vn.edu.kma.traceability_core_service.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Khớp {@code PublicRouteMatcher} trên gateway (servlet path trực tiếp tới
 * product: {@code /api/v1/...}).
 */
public final class ProductPublicPathMatcher {

    private ProductPublicPathMatcher() {
    }

    public static boolean isPublicUnauthenticated(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (path.contains("/api/v1/auth/")) {
            return true;
        }

        if (path.equals("/error")) {
            return true;
        }

        if (path.startsWith("/ws")) {
            return true;
        }

        if (path.contains("/api/v1/internal/")) {
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
                || path.matches("/api/v1/products/[^/]+/reviews$")
                || path.matches("/api/v1/products/[^/]+/review-summary$")
                || path.matches("/api/v1/material-catalog/?$")
                || path.matches("/api/v1/units/[^/]+/trace$")
                || path.matches("/api/v1/units/[^/]+/qr$")
                || path.matches("/api/v1/units/trace/by-serial$")
                || path.matches("/api/v1/units/trace/by-serial/verify$")
                || path.matches("/api/v1/pallets/[^/]+/trace-direct$")
                || path.matches("/api/v1/pallets/[^/]+/verify-direct$")
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

