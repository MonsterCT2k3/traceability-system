package vn.edu.kma.trade_logistics_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.trade_logistics_service.dto.request.TransferInitRequest;

import java.util.List;
import java.util.Map;

@FeignClient(name = "traceability-core-service", url = "${app.url.product-service:http://localhost:8082}")
public interface ProductClient {

    @GetMapping("/api/v1/internal/products/raw-batch/{id}")
    ApiResponse<Map<String, Object>> getRawBatch(@PathVariable("id") String id);

    @GetMapping("/api/v1/internal/products/pallet/{id}")
    ApiResponse<Map<String, Object>> getPallet(@PathVariable("id") String id);

    @GetMapping("/api/v1/internal/products/carton/{id}")
    ApiResponse<Map<String, Object>> getCarton(@PathVariable("id") String id);

    @GetMapping("/api/v1/internal/products/unit/{id}")
    ApiResponse<Map<String, Object>> getProductUnit(@PathVariable("id") String id);

    @PostMapping("/api/v1/internal/products/ownership/transfer")
    ApiResponse<String> transferOwnership(
            @RequestParam("targetType") String targetType,
            @RequestParam("targetId") String targetId,
            @RequestParam("newOwnerId") String newOwnerId
    );

    @PostMapping("/api/v1/internal/products/pallet/{id}/input-status")
    ApiResponse<Map<String, Object>> updatePalletInputStatus(
            @PathVariable("id") String id,
            @RequestParam("ownerId") String ownerId,
            @RequestParam("status") String status
    );

    @PostMapping("/api/v1/internal/products/trade/ship-cartons")
    ApiResponse<List<Map<String, Object>>> shipCartons(
            @RequestParam("sellerId") String sellerId,
            @RequestParam("productId") String productId,
            @RequestParam("quantity") int quantity
    );

    @PostMapping("/api/v1/internal/products/trade/deliver-cartons")
    ApiResponse<List<Map<String, Object>>> deliverCartons(
            @RequestParam("sellerId") String sellerId,
            @RequestParam("buyerId") String buyerId,
            @RequestParam("productId") String productId,
            @RequestParam("quantity") int quantity
    );

    @GetMapping("/api/v1/internal/products/trade/check-inventory")
    ApiResponse<Boolean> checkInventory(
            @RequestParam("sellerId") String sellerId,
            @RequestParam("productId") String productId,
            @RequestParam("quantity") int quantity
    );
}
