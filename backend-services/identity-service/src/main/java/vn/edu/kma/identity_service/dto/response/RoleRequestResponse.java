package vn.edu.kma.identity_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestResponse {
    private String id;
    private String userId;
    private String username; // Lấy thêm từ User để admin dễ nhìn
    private String fullName;
    private String requestedRole;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
