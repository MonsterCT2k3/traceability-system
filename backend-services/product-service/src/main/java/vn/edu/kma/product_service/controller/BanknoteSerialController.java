package vn.edu.kma.product_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.config.OpenApiConfig;
import vn.edu.kma.product_service.dto.request.BanknoteSerialBulkRequest;
import vn.edu.kma.product_service.dto.response.BanknoteSerialBulkSaveResponse;
import vn.edu.kma.product_service.dto.response.BanknoteSerialSummaryResponse;
import vn.edu.kma.product_service.service.BanknoteSerialService;

@RestController
@RequestMapping("/api/v1/banknote-serials")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class BanknoteSerialController {

    private final BanknoteSerialService banknoteSerialService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANUFACTURER','ADMIN')")
    public ResponseEntity<ApiResponse<BanknoteSerialSummaryResponse>> getSummary(
            @RequestHeader("Authorization") String token
    ) {
        try {
            BanknoteSerialSummaryResponse result = banknoteSerialService.getSummary(token);
            return ResponseEntity.ok(ApiResponse.<BanknoteSerialSummaryResponse>builder()
                    .code(200)
                    .message("Lấy tổng quan seri thành công")
                    .result(result)
                    .build());
        } catch (Exception e) {
            log.error("getSummary banknote serials failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<BanknoteSerialSummaryResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Đăng ký hàng loạt seri tờ tiền (mỗi seri một bản ghi). Trùng DB thì bỏ qua (skippedDuplicate).
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('MANUFACTURER','ADMIN')")
    public ResponseEntity<ApiResponse<BanknoteSerialBulkSaveResponse>> bulkRegister(
            @RequestBody BanknoteSerialBulkRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            BanknoteSerialBulkSaveResponse result = banknoteSerialService.bulkRegister(request, token);
            return ResponseEntity.ok(ApiResponse.<BanknoteSerialBulkSaveResponse>builder()
                    .code(200)
                    .message("Đã xử lý danh sách seri")
                    .result(result)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<BanknoteSerialBulkSaveResponse>builder()
                            .code(400)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("bulkRegister banknote serials failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<BanknoteSerialBulkSaveResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }
}
