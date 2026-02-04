package vn.edu.kma.product_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.repository.ProductRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<List<Product>>builder()
                .code(200)
                .message("Lấy danh sách sản phẩm thành công")
                .result(productRepository.findAll())
                .build()
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Product>builder()
                .code(201)
                .message("Tạo sản phẩm thành công")
                .result(productRepository.save(product))
                .build()
        );
    }
}
