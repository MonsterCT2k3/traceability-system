package vn.edu.kma.product_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.CartonCreateRequest;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.service.CartonService;

@RestController
@RequestMapping("/api/v1/pallets")
@RequiredArgsConstructor
@Slf4j
public class CartonController {

    private final CartonService cartonService;

    /**
     * Tạo carton (thùng con) gắn với một pallet đã anchor.
     * Owner carton = owner pallet; chỉ owner pallet mới được tạo.
     */
    @PostMapping("/{palletId}/cartons")
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
}
