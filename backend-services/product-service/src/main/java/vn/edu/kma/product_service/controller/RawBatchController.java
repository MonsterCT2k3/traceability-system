package vn.edu.kma.product_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.edu.kma.product_service.config.OpenApiConfig;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.RawBatchCreateRequest;
import vn.edu.kma.product_service.dto.request.RawBatchMergeRequest;
import vn.edu.kma.product_service.dto.response.RawBatchResponse;
import vn.edu.kma.product_service.service.RawBatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/raw-batches")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class RawBatchController {

    private final RawBatchService rawBatchService;

    /**
     * Tạo lô nguyên liệu gốc (RAW) lên on-chain bằng recordBatch.
     * Trả về txHash để bạn lưu/match sau này.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER','ADMIN')")
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

    /**
     * Gộp nhiều lô cùng loại: DB trước (xóa lô nguồn, tạo lô mới), sau đó recordBatch; lỗi chain thì hoàn tác DB.
     */
    @PostMapping("/merge")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> mergeRawBatches(
            @RequestBody RawBatchMergeRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Map<String, String> result = rawBatchService.mergeRawBatches(request, token);
            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .code(200)
                    .message("Đã tạo lô mới (gộp) và ghi lên blockchain")
                    .result(result)
                    .build());
        } catch (Exception e) {
            log.error("mergeRawBatches failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, String>>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER','ADMIN')")
    public ResponseEntity<ApiResponse<List<RawBatchResponse>>> getMyRawBatches(
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<RawBatchResponse> result = rawBatchService.getMyRawBatches(token);
            return ResponseEntity.ok(ApiResponse.<List<RawBatchResponse>>builder()
                    .code(200)
                    .message("Lấy danh sách lô nguyên liệu thành công")
                    .result(result)
                    .build());
        } catch (Exception e) {
            log.error("getMyRawBatches failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<List<RawBatchResponse>>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }
}

