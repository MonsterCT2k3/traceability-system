package vn.edu.kma.identity_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ hiện các trường không null
public class ApiResponse<T> {
    private int code;       // Mã lỗi nội bộ (ví dụ: 1000 cho thành công)
    private String message; // Thông báo
    private T result;       // Dữ liệu thực tế (User, Token, List...)
}
