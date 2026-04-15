package vn.edu.kma.product_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import vn.edu.kma.common.security.UserRole;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

/**
 * Xác thực JWT (cùng secret với identity). Không kiểm tra blacklist — gateway/identity đã introspect;
 * đây là lớp phòng thủ khi gọi thẳng product (port 8082).
 */
@Service
public class JwtTokenService {

    @Value("${jwt.signerKey}")
    private String secretKey;

    public Optional<UsernamePasswordAuthenticationToken> parseAccessToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(rawToken.trim())
                    .getBody();
            String username = claims.getSubject();
            if (username == null) {
                return Optional.empty();
            }
            String roleClaim = claims.get("role", String.class);
            UserRole role = UserRole.fromClaimOrDefault(roleClaim);
            var authority = new SimpleGrantedAuthority(role.springAuthority());
            return Optional.of(new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    Collections.singletonList(authority)
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
