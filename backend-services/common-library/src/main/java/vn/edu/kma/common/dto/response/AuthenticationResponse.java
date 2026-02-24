package vn.edu.kma.common.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String accessToken; // Đây là Access Token (AT)
    private String refreshToken; // Đây là Refresh Token (RT)
}
