package vn.edu.kma.product_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.edu.kma.product_service.config.OpenApiConfig;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.PalletAnchorRequest;
import vn.edu.kma.product_service.service.PalletService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PalletController {

    private final PalletService palletService;

    /**
     * Anchor transformed-batch lên chain.
     * Lưu Pallet vào DB và cập nhật Product.chainBatchIdHex (để transfer hiện tại vẫn hoạt động).
     */
    @PostMapping("/{productId}/pallets/anchor")
    @PreAuthorize("hasAnyRole('MANUFACTURER','ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> anchorPallet(
            @PathVariable String productId,
            @RequestBody PalletAnchorRequest request,
            @RequestHeader("Authorization") String tokenHeader
    ) {
        try {
            Map<String, String> result = palletService.anchorPallet(productId, request, tokenHeader);

            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .code(200)
                    .message("Đã anchor pallet lên blockchain")
                    .result(result)
                    .build());

        } catch (Exception e) {
            log.error("anchorPallet failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, String>>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }
}

