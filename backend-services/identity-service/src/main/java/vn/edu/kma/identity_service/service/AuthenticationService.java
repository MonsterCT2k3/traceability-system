package vn.edu.kma.identity_service.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.kma.common.dto.request.AuthenticationRequest;
import vn.edu.kma.common.dto.request.IntrospectRequest;
import vn.edu.kma.common.dto.request.RefreshRequest;
import vn.edu.kma.common.dto.response.AuthenticationResponse;
import vn.edu.kma.common.dto.response.IntrospectResponse;
import vn.edu.kma.common.exception.AppException;
import vn.edu.kma.common.exception.ErrorCode;
import vn.edu.kma.identity_service.dto.response.UserResponse;
import vn.edu.kma.identity_service.entity.InvalidatedToken;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.kma.identity_service.repository.InvalidatedTokenRepository;
import vn.edu.kma.common.dto.request.LogoutRequest;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final PasswordEncoder passwordEncoder; // Tiêm bộ mã hóa vào đây

    @Value("${jwt.signerKey}")
    private String SECRET_KEY;

    public AuthenticationResponse login(AuthenticationRequest request) {
        // 1. Tìm user trong DB
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!"));

        // 2. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        // 3. Tạo Access Token (Thời gian ngắn - ví dụ 1 giờ)
        String accessToken = Jwts.builder()
                .setId(UUID.randomUUID().toString()) // JTI: ID duy nhất của Token
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 giờ
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        // 4. Tạo Refresh Token (Thời gian dài - ví dụ 7 ngày)
        String refreshToken = Jwts.builder()
                .setId(UUID.randomUUID().toString()) // JTI riêng cho RT
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 604800000)) // 7 ngày
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        // 5. Trả về cặp bài trùng
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public UserResponse register(User user) {
        // Kiểm tra trùng username
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        User savedUser = userRepository.save(user);

        // Chuyển đổi từ Entity sang Response DTO (Có thể dùng MapStruct để làm tự động sau này)
        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .build();
    }

    public AuthenticationResponse refresh(RefreshRequest request) {
        try {
            // 1. Giải mã và kiểm tra chữ ký/hạn dùng của Refresh Token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                    .build()
                    .parseClaimsJws(request.getRefreshToken())
                    .getBody();

            // 2. Kiểm tra xem Token này có bị nằm trong danh sách đen (Logout/Đã dùng) không
            String jti = claims.getId();
            if (invalidatedTokenRepository.existsById(jti)) {
                throw new RuntimeException("Token đã hết hiệu lực hoặc đã được sử dụng!");
            }

            // 3. Lấy thông tin User từ Token
            String username = claims.getSubject();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

            // 4. Cấp Access Token mới (Thời gian ngắn)
            String newAccessToken = Jwts.builder()
                    .setId(UUID.randomUUID().toString())
                    .setSubject(user.getUsername())
                    .claim("role", user.getRole())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 giờ
                    .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                    .compact();

            // 5. Trả về Response (Giữ nguyên Refresh Token cũ hoặc cấp mới - Rotation)
            return AuthenticationResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(request.getRefreshToken()) // Ở đây mình trả lại RT cũ cho đơn giản
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Refresh Token không hợp lệ hoặc đã hết hạn!");
        }
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            // 1. Giải mã Token để lấy thông tin (Sử dụng thư viện Nimbus như đã làm ở Gateway)
            SignedJWT signedJWT = SignedJWT.parse(request.getToken());

            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            // 2. Lưu JTI và ExpiryTime vào danh sách đen (Postgres)
            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jti)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);

        } catch (ParseException e) {
            log.error("Token không hợp lệ hoặc đã hết hạn");
            // Nếu token đã hết hạn hoặc lỗi, coi như đã logout thành công
        }
    }

    public IntrospectResponse introspect(IntrospectRequest request) {
        String token = request.getToken();
        boolean isValid = true;

        try {
            // 1. Kiểm tra chữ ký và hạn dùng
            verifyToken(token);
        } catch (Exception e) {
            // Nếu lỗi (hết hạn hoặc sai chữ ký), đánh dấu không hợp lệ
            isValid = false;
        }

        // 2. Nếu vẫn tạm hợp lệ, kiểm tra xem có nằm trong Blacklist (Database) không
        if (isValid) {
            try {
                SignedJWT signedJWT = SignedJWT.parse(token);
                String jti = signedJWT.getJWTClaimsSet().getJWTID();

                // Nếu tìm thấy JTI trong bảng InvalidatedToken, nghĩa là đã Logout
                if (invalidatedTokenRepository.existsById(jti)) {
                    isValid = false;
                }
            } catch (Exception e) {
                isValid = false;
            }
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    // Hàm hỗ trợ verify chữ ký (Nên tách riêng để dùng lại cho Logout/Refresh)
    private void verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SECRET_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Kiểm tra chữ ký
        if (!signedJWT.verify(verifier)) {
            throw new RuntimeException("Chữ ký Token không hợp lệ!");
        }

        // Kiểm tra thời gian hết hạn
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (expiryTime.before(new Date())) {
            throw new RuntimeException("Token đã hết hạn!");
        }
    }
}
