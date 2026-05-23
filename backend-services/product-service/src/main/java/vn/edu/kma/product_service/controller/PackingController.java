package vn.edu.kma.product_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.config.OpenApiConfig;
import vn.edu.kma.product_service.dto.response.ProductPackingManifestResponse;
import vn.edu.kma.product_service.dto.response.ProductPackingSummaryResponse;
import vn.edu.kma.product_service.service.PackingService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cartons/packing")
@RequiredArgsConstructor
public class PackingController {

    private final PackingService packingService;

    @GetMapping("/summary/my")
    @PreAuthorize("hasAnyRole('MANUFACTURER','ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<ApiResponse<List<ProductPackingSummaryResponse>>> getMyPackingSummary(
            @RequestHeader("Authorization") String tokenHeader
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<List<ProductPackingSummaryResponse>>builder()
                .code(200)
                .message("Lấy thống kê thành phẩm đã đóng gói thành công")
                .result(packingService.getMyPackingSummary(tokenHeader))
                .build());
    }

    @GetMapping("/manifest/my")
    @PreAuthorize("hasAnyRole('MANUFACTURER','ADMIN')")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<ApiResponse<List<ProductPackingManifestResponse>>> getMyPackingManifest(
            @RequestHeader("Authorization") String tokenHeader
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<List<ProductPackingManifestResponse>>builder()
                .code(200)
                .message("Lấy manifest thùng và serial đã đóng gói thành công")
                .result(packingService.getMyPackingManifest(tokenHeader))
                .build());
    }
}
