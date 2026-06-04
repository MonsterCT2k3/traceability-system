package vn.edu.kma.traceability_core_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class ClaimTokenService {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();

    @Value("${app.claim.hmac-secret}")
    private String secret;

    public String generate() {
        StringBuilder token = new StringBuilder("CLM-");
        for (int i = 0; i < 28; i++) token.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        return token.toString();
    }

    public String hash(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(normalize(token).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Khong the bam claim token", e);
        }
    }

    public String normalize(String token) {
        if (token == null || !token.trim().toUpperCase().matches("^CLM-[A-Z2-9]{20,64}$")) {
            throw new RuntimeException("Claim QR khong hop le");
        }
        return token.trim().toUpperCase();
    }
}
