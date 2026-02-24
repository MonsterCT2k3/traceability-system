package vn.edu.kma.common.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Thêm @Data để có Getter, Setter, toString, etc.
@Builder
@NoArgsConstructor // Quan trọng: Cần thiết cho Jackson deserialization
@AllArgsConstructor
public class IntrospectRequest {
    private String token;
}
