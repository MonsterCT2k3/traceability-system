package vn.edu.kma.product_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri.contains("/swagger-ui") || uri.contains("/v3/api-docs") || uri.contains("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!ProductPublicPathMatcher.isPublicUnauthenticated(request)) {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                jwtTokenService.parseAccessToken(token)
                        .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
            }
        }

        filterChain.doFilter(request, response);
    }
}
