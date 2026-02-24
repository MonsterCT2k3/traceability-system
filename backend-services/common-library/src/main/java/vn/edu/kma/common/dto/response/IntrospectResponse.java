package vn.edu.kma.common.dto.response;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ hiện các trường không null
public class IntrospectResponse {
    private boolean valid; // true nếu token OK, false nếu đã Logout/Hết hạn
}