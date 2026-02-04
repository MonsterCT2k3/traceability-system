package vn.edu.kma.identity_service.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.kma.identity_service.dto.response.UserResponse;
import vn.edu.kma.identity_service.entity.User;
import vn.edu.kma.identity_service.exception.AppException;
import vn.edu.kma.identity_service.exception.ErrorCode;
import vn.edu.kma.identity_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Tiêm bộ mã hóa vào đây

    @Value("${jwt.signerKey}")
    private String SECRET_KEY;

    public String login(String username, String password) {
        // 1. Tìm user trong DB
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        // 3. Tạo JWT Token nếu mật khẩu đúng
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // Hết hạn sau 1 ngày
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
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
}
