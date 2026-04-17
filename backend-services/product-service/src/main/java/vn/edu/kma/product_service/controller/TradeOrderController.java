package vn.edu.kma.product_service.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.config.OpenApiConfig;
import vn.edu.kma.product_service.dto.request.AssignCarrierRequest;
import vn.edu.kma.product_service.dto.request.TradeOrderCreateRequest;
import vn.edu.kma.product_service.dto.response.TradeOrderResponse;
import vn.edu.kma.product_service.service.TradeOrderService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANUFACTURER','RETAILER')")
    public ResponseEntity<ApiResponse<TradeOrderResponse>> create(
            @RequestBody TradeOrderCreateRequest request,
            @RequestHeader("Authorization") String token
    ) {
        try {
            TradeOrderResponse r = tradeOrderService.createOrder(request, token);
            return ResponseEntity.ok(ApiResponse.<TradeOrderResponse>builder()
                    .code(200)
                    .message("Tạo đơn thành công")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("create order failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<TradeOrderResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TradeOrderResponse>> getById(
            @PathVariable String id,
            @RequestHeader("Authorization") String token
    ) {
        try {
            TradeOrderResponse r = tradeOrderService.getById(id, token);
            return ResponseEntity.ok(ApiResponse.<TradeOrderResponse>builder()
                    .code(200)
                    .message("OK")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("get order failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<TradeOrderResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/mine/buyer")
    @PreAuthorize("hasAnyRole('MANUFACTURER','RETAILER')")
    public ResponseEntity<ApiResponse<List<TradeOrderResponse>>> listBuyer(
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<TradeOrderResponse> r = tradeOrderService.listAsBuyer(token);
            return ResponseEntity.ok(ApiResponse.<List<TradeOrderResponse>>builder()
                    .code(200)
                    .message("OK")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("list buyer orders failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<List<TradeOrderResponse>>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/mine/seller")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER')")
    public ResponseEntity<ApiResponse<List<TradeOrderResponse>>> listSeller(
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<TradeOrderResponse> r = tradeOrderService.listAsSeller(token);
            return ResponseEntity.ok(ApiResponse.<List<TradeOrderResponse>>builder()
                    .code(200)
                    .message("OK")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("list seller orders failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<List<TradeOrderResponse>>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/mine/carrier")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public ResponseEntity<ApiResponse<List<TradeOrderResponse>>> listCarrier(
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<TradeOrderResponse> r = tradeOrderService.listAsCarrier(token);
            return ResponseEntity.ok(ApiResponse.<List<TradeOrderResponse>>builder()
                    .code(200)
                    .message("OK")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("list carrier orders failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<List<TradeOrderResponse>>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER')")
    public ResponseEntity<ApiResponse<TradeOrderResponse>> accept(
            @PathVariable String id,
            @RequestHeader("Authorization") String token
    ) {
        try {
            TradeOrderResponse r = tradeOrderService.accept(id, token);
            return ResponseEntity.ok(ApiResponse.<TradeOrderResponse>builder()
                    .code(200)
                    .message("Đã chấp nhận đơn")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("accept order failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<TradeOrderResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER')")
    public ResponseEntity<ApiResponse<TradeOrderResponse>> reject(
            @PathVariable String id,
            @RequestHeader("Authorization") String token
    ) {
        try {
            TradeOrderResponse r = tradeOrderService.reject(id, token);
            return ResponseEntity.ok(ApiResponse.<TradeOrderResponse>builder()
                    .code(200)
                    .message("Đã từ chối đơn")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("reject order failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<TradeOrderResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANUFACTURER','RETAILER')")
    public ResponseEntity<ApiResponse<TradeOrderResponse>> cancel(
            @PathVariable String id,
            @RequestHeader("Authorization") String token
    ) {
        try {
            TradeOrderResponse r = tradeOrderService.cancel(id, token);
            return ResponseEntity.ok(ApiResponse.<TradeOrderResponse>builder()
                    .code(200)
                    .message("Đã hủy đơn")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("cancel order failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<TradeOrderResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/{id}/assign-carrier")
    @PreAuthorize("hasAnyRole('SUPPLIER','MANUFACTURER')")
    public ResponseEntity<ApiResponse<TradeOrderResponse>> assignCarrier(
            @PathVariable String id,
            @RequestBody AssignCarrierRequest body,
            @RequestHeader("Authorization") String token
    ) {
        try {
            TradeOrderResponse r = tradeOrderService.assignCarrier(id, body, token);
            return ResponseEntity.ok(ApiResponse.<TradeOrderResponse>builder()
                    .code(200)
                    .message("Đã gán đơn vị vận chuyển")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("assign carrier failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<TradeOrderResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/{id}/confirm-picked-up")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public ResponseEntity<ApiResponse<TradeOrderResponse>> confirmPickedUp(
            @PathVariable String id,
            @RequestHeader("Authorization") String token
    ) {
        try {
            TradeOrderResponse r = tradeOrderService.confirmPickedUpFromSeller(id, token);
            return ResponseEntity.ok(ApiResponse.<TradeOrderResponse>builder()
                    .code(200)
                    .message("Đã xác nhận nhận hàng từ người bán")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("confirm picked up failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<TradeOrderResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/{id}/confirm-delivered")
    @PreAuthorize("hasAnyRole('TRANSPORTER','SUPPLIER','MANUFACTURER')")
    public ResponseEntity<ApiResponse<TradeOrderResponse>> confirmDelivered(
            @PathVariable String id,
            @RequestHeader("Authorization") String token
    ) {
        try {
            TradeOrderResponse r = tradeOrderService.confirmDelivered(id, token);
            return ResponseEntity.ok(ApiResponse.<TradeOrderResponse>builder()
                    .code(200)
                    .message("Đã xác nhận giao hàng")
                    .result(r)
                    .build());
        } catch (Exception e) {
            log.error("confirm delivered failed", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<TradeOrderResponse>builder()
                            .code(500)
                            .message("Lỗi: " + e.getMessage())
                            .build()
            );
        }
    }
}
