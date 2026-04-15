package vn.edu.kma.common.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body cho API admin cập nhật role user (giá trị = {@link vn.edu.kma.common.security.UserRole#name()}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {
    private String role;
}
