package vn.edu.kma.product_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Đăng ký vào {@link org.springframework.security.web.SecurityFilterChain} — cùng quy tắc với gateway.
 */
@Component
public class ProductPublicRequestMatcher implements RequestMatcher {

    @Override
    public boolean matches(HttpServletRequest request) {
        return ProductPublicPathMatcher.isPublicUnauthenticated(request);
    }
}
