package vn.edu.kma.product_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.RawBatchCreateRequest;
import vn.edu.kma.product_service.service.RawBatchService;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/raw-batches")
@RequiredArgsConstructor
@Slf4j
public class RawBatchController {

    private final RawBatchService rawBatchService;

    /**
     * Tạo lô nguyên liệu gốc (RAW) lên on-chain bằng recordBatch.
     * Trả về txHash để bạn lưu/match sau này.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createRawBatch(
            @RequestBody RawBatchCreateRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Map<String, String> result = rawBatchService.createRawBatch(request, token);

            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .code(200)
                    .message("Đã ghi RAW batch lên blockchain")
                    .result(result)
                    .build());

        } catch (Exception e) {
            log.error("createRawBatch failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, String>>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }
}

