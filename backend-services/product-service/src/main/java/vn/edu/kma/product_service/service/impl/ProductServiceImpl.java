package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.kma.product_service.dto.request.ProductRequest;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.service.ProductService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Override
    public Product createProduct(ProductRequest request, String token) {
        try {
            // 1. Lấy User ID từ Token (Logic tách biệt)
            String userId = extractUserIdFromToken(token);

            // 2. Map từ DTO sang Entity (Có thể dùng MapStruct sau này)
            Product product = Product.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .quantity(request.getQuantity())
                    .imageUrl(request.getImageUrl())
                    .ownerId(userId) // Gán chủ sở hữu
                    .build();

            // 3. Lưu vào DB
            return productRepository.save(product);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo sản phẩm: " + e.getMessage());
        }
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(String id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
    }

    private String extractUserIdFromToken(String token) throws Exception {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}
