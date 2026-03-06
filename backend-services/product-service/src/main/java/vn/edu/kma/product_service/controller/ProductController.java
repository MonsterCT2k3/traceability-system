package vn.edu.kma.product_service.controller;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.ProductRequest;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.service.ProductService;
import vn.edu.kma.product_service.utils.QRCodeGenerator;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    private final ProductService productService;
    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<List<Product>>builder()
                .code(200)
                .message("Lấy danh sách sản phẩm thành công")
                .result(productService.getAllProducts())
                .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Product>builder().
                code(200)
                .message("Lấy sản phẩm thành công")
                .result(productService.getProductById(id))
                .build()
        );
    }
    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody ProductRequest request,
                                                              @RequestHeader("Authorization") String tokenHeader) throws ParseException {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Product>builder()
                .code(201)
                .message("Tạo sản phẩm thành công")
                .result(productService.createProduct(request, tokenHeader))
                .build()
        );
    }

    // Thêm API mới này vào trong class ProductController
    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQRCode(@PathVariable String id) {
        try {
            // Kiểm tra sản phẩm có tồn tại không (Optional - Tốt nhất nên có)
            // productService.getProductById(id);

            // URL mà người dùng sẽ truy cập khi quét mã
            // Ví dụ: https://traceability.vn/view?id=...
            // Hiện tại mình chưa có domain thật nên để localhost hoặc một đường dẫn giả định
            String content = "http://localhost:3000/product/" + id;

            // Tạo ảnh QR kích thước 300x300
            byte[] qrImage = QRCodeGenerator.generateQRCodeImage(content, 300, 300);

            return ResponseEntity.ok().body(qrImage);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
