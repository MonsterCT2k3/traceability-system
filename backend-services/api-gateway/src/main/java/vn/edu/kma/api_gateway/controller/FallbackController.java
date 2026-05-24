package vn.edu.kma.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.kma.common.dto.response.ApiResponse;

@RestController
public class FallbackController {

    @RequestMapping({"/fallback", "fallback", "/fallback/**"})
    public ResponseEntity<ApiResponse<Void>> fallback() {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(503)
                .message("Hệ thống đang quá tải hoặc tạm thời gián đoạn. Vui lòng thử lại sau.")
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
