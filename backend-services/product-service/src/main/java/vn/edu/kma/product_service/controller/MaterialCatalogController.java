package vn.edu.kma.product_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.response.MaterialCategoryCatalogResponse;
import vn.edu.kma.product_service.service.MaterialCatalogService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/material-catalog")
@RequiredArgsConstructor
public class MaterialCatalogController {

    private final MaterialCatalogService materialCatalogService;

    /**
     * Danh mục nguyên liệu (loại → danh sách tên). Đọc công khai trên product-service.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialCategoryCatalogResponse>>> getCatalog() {
        List<MaterialCategoryCatalogResponse> list = materialCatalogService.getCatalog();
        return ResponseEntity.ok(ApiResponse.<List<MaterialCategoryCatalogResponse>>builder()
                .code(200)
                .message("OK")
                .result(list)
                .build());
    }
}
