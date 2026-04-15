package vn.edu.kma.identity_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.kma.identity_service.service.AuthenticationService;

import java.io.IOException;

/**
 * Đọc Bearer access token, xác thực chữ ký + hạn + blacklist, gán {@link org.springframework.security.core.Authentication}
 * để {@code hasRole("ADMIN")} hoạt động trên {@code /api/v1/admin/**}.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationService authenticationService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.contains("/api/v1/auth/")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.contains("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            authenticationService.authenticateAccessToken(token)
                    .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
        }

        filterChain.doFilter(request, response);
    }
}
