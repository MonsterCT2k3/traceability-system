package vn.edu.kma.blockchain_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.blockchain_service.service.TraceabilityService;
import vn.edu.kma.common.dto.response.ApiResponse; // Dùng lại ApiResponse của common-library

import java.util.Map;

@RestController
@RequestMapping("/api/v1/blockchain")
@RequiredArgsConstructor
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    // API Deploy Contract (Chỉ chạy 1 lần để lấy địa chỉ)
    @PostMapping("/deploy")
    public ResponseEntity<ApiResponse<String>> deployContract() {
        try {
            String address = traceabilityService.deployContract();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .code(200)
                    .message("Deploy thành công")
                    .result(address)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .code(500)
                    .message("Lỗi: " + e.getMessage())
                    .build());
        }
    }

    // API Ghi nhật ký (Product Service sẽ gọi API này)
    @PostMapping("/history")
    public ResponseEntity<ApiResponse<String>> addHistory(@RequestBody Map<String, String> request) {
        try {
            String productId = request.get("productId");
            String action = request.get("action");
            String description = request.get("description");

            String txHash = traceabilityService.addHistory(productId, action, description);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .code(200)
                    .message("Ghi Blockchain thành công")
                    .result(txHash)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .code(500)
                    .message("Lỗi Blockchain: " + e.getMessage())
                    .build());
        }
    }
}
