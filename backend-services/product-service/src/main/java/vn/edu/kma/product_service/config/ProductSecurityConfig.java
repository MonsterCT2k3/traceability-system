package vn.edu.kma.product_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.common.exception.ErrorCode;
import vn.edu.kma.product_service.security.JwtAuthenticationFilter;
import vn.edu.kma.product_service.security.ProductPublicRequestMatcher;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ProductSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ProductPublicRequestMatcher productPublicRequestMatcher;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain productSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers(productPublicRequestMatcher).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            var body = ApiResponse.builder()
                                    .code(ErrorCode.UNAUTHENTICATED.getCode())
                                    .message("Chưa đăng nhập hoặc token không hợp lệ")
                                    .build();
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            var body = ApiResponse.builder()
                                    .code(ErrorCode.FORBIDDEN.getCode())
                                    .message(ErrorCode.FORBIDDEN.getMessage())
                                    .build();
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        }));

        return http.build();
    }
}
