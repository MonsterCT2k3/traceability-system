package vn.edu.kma.product_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.HistoryRequest;
import vn.edu.kma.product_service.entity.ProductHistory;
import vn.edu.kma.product_service.service.ProductHistoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/histories")
@RequiredArgsConstructor
public class ProductHistoryController {
    private final ProductHistoryService historyService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductHistory>> createHistory(
            @RequestBody HistoryRequest request,
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ProductHistory>builder()
                .code(201)
                .message("Ghi nhật ký thành công")
                .result(historyService.createHistory(request, token))
                .build()
        );
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ProductHistory>>> getHistoryByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.<List<ProductHistory>>builder()
                .code(200)
                .result(historyService.getHistoryByProductId(productId))
                .build()
        );
    }
}
