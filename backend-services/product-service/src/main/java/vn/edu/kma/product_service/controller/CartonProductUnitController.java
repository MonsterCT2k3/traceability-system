package vn.edu.kma.product_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.kma.product_service.config.OpenApiConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.ProductUnitGenerateRequest;
import vn.edu.kma.product_service.dto.response.ProductUnitGenerateResponse;
import vn.edu.kma.product_service.service.ProductUnitService;

@RestController
@RequestMapping("/api/v1/cartons")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CartonProductUnitController {

    private final ProductUnitService productUnitService;

    /**
     * Sinh hàng loạt unit trong một carton: unitSerial có quy tắc, secret chỉ trả một lần trong response để in scratch.
     */
    @PostMapping("/{cartonId}/units/generate")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER','ADMIN')")
    public ResponseEntity<ApiResponse<ProductUnitGenerateResponse>> generateUnits(
            @PathVariable String cartonId,
            @RequestBody ProductUnitGenerateRequest request,
            @RequestHeader("Authorization") String tokenHeader
    ) {
        try {
            ProductUnitGenerateResponse result = productUnitService.generateUnits(cartonId, request, tokenHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.<ProductUnitGenerateResponse>builder()
                            .code(201)
                            .message("Đã sinh unit; secret chỉ hiển thị lần này — hãy lưu/export trước khi đóng")
                            .result(result)
                            .build()
            );
        } catch (Exception e) {
            log.error("generateUnits failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<ProductUnitGenerateResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }
}
