package vn.edu.kma.api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vn.edu.kma.common.dto.request.IntrospectRequest;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.common.dto.response.IntrospectResponse;

import java.io.IOException;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthenticationFilter implements Filter {

    @Value("${app.jwt.signer-key}")
    private String SIGNER_KEY;

    @Value("${app.services.identity.url}")
    private String identityServiceUrl;

    private final RestTemplate restTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath();

        // Loại biên các API Public (Login, Register, Introspect)
        if (path.contains("/api/v1/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // GỌI SANG IDENTITY SERVICE ĐỂ KIỂM TRA
            IntrospectRequest introspectRequest = new IntrospectRequest(token);

            try {
                // Sử dụng URL từ cấu hình thay vì hardcode
                var identityResponse = restTemplate.postForObject(identityServiceUrl, introspectRequest, ApiResponse.class);

                // Kiểm tra phản hồi từ Identity Service
                if (identityResponse != null && identityResponse.getCode() == 200) {
                    // Convert kết quả trả về sang IntrospectResponse để check biến 'valid'
                    ObjectMapper mapper = new ObjectMapper();
                    IntrospectResponse introspectResponse = mapper.convertValue(identityResponse.getResult(), IntrospectResponse.class);

                    if (introspectResponse != null && introspectResponse.isValid()) {
                        chain.doFilter(request, response);
                        return; // Chỉ cho qua khi valid = true
                    }
                }

            } catch (Exception e) {
                // Nếu Identity Service chết, Gateway cũng nên chặn để an toàn
                // Log lỗi tại đây (dùng Slf4j)
                log.error("Identity Service is down", e);
            }
        }

        // Nếu không có header, không phải Bearer, hoặc check token thất bại -> Trả về 401
        sendUnauthenticatedResponse((HttpServletResponse) response, "Unauthenticated");
    }

    private void sendUnauthenticatedResponse(HttpServletResponse response, String message) throws IOException {
        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .code(401)
                .message(message)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Dùng ObjectMapper để chuyển ApiResponse sang JSON String
        response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
    }
}
