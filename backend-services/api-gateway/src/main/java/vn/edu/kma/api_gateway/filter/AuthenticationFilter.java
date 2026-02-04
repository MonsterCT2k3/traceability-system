package vn.edu.kma.api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edu.kma.common.dto.response.ApiResponse;

import java.io.IOException;
import java.util.Date;

@Component
public class AuthenticationFilter implements Filter {

    @Value("${app.jwt.signer-key}")
    private String SIGNER_KEY;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getServletPath();

        // 1. Cho phép các request đăng nhập/đăng ký đi qua mà không cần token
        if (path.contains("/api/v1/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Kiểm tra Header Authorization
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\": 401, \"message\": \"Missing or invalid token\"}");
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 3. Giải mã và Xác minh Chữ ký (Core Cryptography logic)
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

            // Kiểm tra chữ ký và thời hạn Token
            boolean verified = signedJWT.verify(verifier);
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            if (!verified || expirationTime.before(new Date())) {
                sendUnauthenticatedResponse(httpResponse, "Token expired or invalid signature");
                return;
            }

            // (Nâng cao): Bạn có thể lấy thông tin User từ Token để dùng sau này
            // String username = signedJWT.getJWTClaimsSet().getSubject();

        } catch (Exception e) {
            sendUnauthenticatedResponse(httpResponse, "Invalid Token format");
            return;
        }

        chain.doFilter(request, response);
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
