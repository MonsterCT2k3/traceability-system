package vn.edu.kma.product_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Base URL cho QR catalog sản phẩm (web/app mở link). QR unit truy xuất dùng plain {@code unitId}, không qua đây.
 */
@Component
public class TraceFrontendConfig {

    private final String baseUrl;

    public TraceFrontendConfig(
            @Value("${app.trace.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.baseUrl = stripTrailingSlash(frontendBaseUrl == null ? "" : frontendBaseUrl.trim());
    }

    public String productCatalogLandingUrl(String productId) {
        return baseUrl + "/product/" + productId;
    }

    private static String stripTrailingSlash(String url) {
        if (url.isEmpty()) {
            return "http://localhost:3000";
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
