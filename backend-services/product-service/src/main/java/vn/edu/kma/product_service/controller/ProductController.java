package vn.edu.kma.product_service.controller;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.repository.ProductRepository;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
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
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody Product product,
                                                              @RequestHeader("Authorization") String tokenHeader) throws ParseException {
        String ownerId = "unknown";
        try{
            // 2. Cắt bỏ chữ "Bearer " để lấy token thô
            String token = tokenHeader.substring(7);

            // 3. Parse Token để lấy thông tin (Subject/Username)
            SignedJWT signedJWT = SignedJWT.parse(token);
            ownerId = signedJWT.getJWTClaimsSet().getStringClaim("userId");

            log.info("User {} is creating product {}", ownerId, product.getName());
        }catch (Exception e){
            log.error("Error while read token", e);
        }
        product.setOwnerId(ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Product>builder()
                .code(201)
                .message("Tạo sản phẩm thành công")
                .result(productRepository.save(product))
                .build()
        );
    }
}
