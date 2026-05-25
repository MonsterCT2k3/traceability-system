package vn.edu.kma.catalog_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.catalog_service.dto.response.MaterialCategoryCatalogResponse;
import vn.edu.kma.catalog_service.service.MaterialCatalogService;

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

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MaterialCategoryCatalogResponse>>> getMyCatalog(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String ownerId = extractUserId(authHeader);
        List<MaterialCategoryCatalogResponse> list = materialCatalogService.getMyCatalog(ownerId);
        return ResponseEntity.ok(ApiResponse.<List<MaterialCategoryCatalogResponse>>builder()
                .code(200)
                .message("OK")
                .result(list)
                .build());
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<MaterialCategoryCatalogResponse>> createCategory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody vn.edu.kma.catalog_service.dto.request.CreateMaterialCategoryRequest request) {
        String ownerId = extractUserId(authHeader);
        if (ownerId == null) throw new RuntimeException("Unauthorized");
        MaterialCategoryCatalogResponse res = materialCatalogService.createCategory(ownerId, request.getLabel());
        return ResponseEntity.ok(ApiResponse.<MaterialCategoryCatalogResponse>builder()
                .code(200)
                .message("Tạo danh mục nguyên liệu thành công")
                .result(res)
                .build());
    }

    @PostMapping("/categories/{categoryId}/items")
    public ResponseEntity<ApiResponse<vn.edu.kma.catalog_service.dto.response.MaterialItemOptionResponse>> createItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String categoryId,
            @RequestBody vn.edu.kma.catalog_service.dto.request.CreateMaterialItemRequest request) {
        String ownerId = extractUserId(authHeader);
        if (ownerId == null) throw new RuntimeException("Unauthorized");
        vn.edu.kma.catalog_service.dto.response.MaterialItemOptionResponse res = materialCatalogService.createItem(ownerId, categoryId, request.getName());
        return ResponseEntity.ok(ApiResponse.<vn.edu.kma.catalog_service.dto.response.MaterialItemOptionResponse>builder()
                .code(200)
                .message("Tạo tên nguyên liệu thành công")
                .result(res)
                .build());
    }

    private String extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String token = authHeader.substring(7);
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) return null;
            java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(payload);
            return node.has("sub") ? node.get("sub").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
