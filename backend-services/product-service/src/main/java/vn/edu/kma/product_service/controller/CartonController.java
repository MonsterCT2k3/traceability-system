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
import vn.edu.kma.product_service.dto.request.CartonCreateRequest;
import vn.edu.kma.product_service.dto.request.PalletBulkPackingRequest;
import vn.edu.kma.product_service.dto.response.PalletBulkPackingResponse;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.service.CartonService;
import vn.edu.kma.product_service.service.PalletPackingService;

@RestController
@RequestMapping("/api/v1/pallets")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CartonController {

    private final CartonService cartonService;
    private final PalletPackingService palletPackingService;

    /**
     * Tạo carton (thùng con) gắn với một pallet đã anchor.
     * Owner carton = owner pallet; chỉ owner pallet mới được tạo.
     */
    @PostMapping("/{palletId}/cartons")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER','ADMIN')")
    public ResponseEntity<ApiResponse<Carton>> createCarton(
            @PathVariable String palletId,
            @RequestBody CartonCreateRequest request,
            @RequestHeader("Authorization") String tokenHeader
    ) {
        try {
            Carton carton = cartonService.createCarton(palletId, request, tokenHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.<Carton>builder()
                            .code(201)
                            .message("Tạo carton thành công")
                            .result(carton)
                            .build()
            );
        } catch (Exception e) {
            log.error("createCarton failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Carton>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Đóng hàng loạt: tạo N carton trên pallet + sinh M unit/thùng (secret chỉ có trong response, lưu ngay).
     */
    @PostMapping("/{palletId}/packing-bulk")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER','ADMIN')")
    public ResponseEntity<ApiResponse<PalletBulkPackingResponse>> packingBulk(
            @PathVariable String palletId,
            @RequestBody PalletBulkPackingRequest request,
            @RequestHeader("Authorization") String tokenHeader
    ) {
        try {
            PalletBulkPackingResponse result = palletPackingService.bulkPack(palletId, request, tokenHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.<PalletBulkPackingResponse>builder()
                            .code(201)
                            .message("Đã tạo " + result.getCartonsCreated() + " carton và " + result.getUnitsCreated() + " unit")
                            .result(result)
                            .build()
            );
        } catch (Exception e) {
            log.error("packingBulk failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<PalletBulkPackingResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }
}
