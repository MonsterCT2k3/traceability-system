package vn.edu.kma.catalog_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import vn.edu.kma.catalog_service.service.IdentityClient;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.kma.catalog_service.dto.request.ProductRequest;
import vn.edu.kma.catalog_service.entity.Product;
import vn.edu.kma.catalog_service.entity.Product;
import vn.edu.kma.catalog_service.repository.ProductRepository;
import vn.edu.kma.catalog_service.service.CloudinaryService;
import vn.edu.kma.catalog_service.service.ProductService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;
    private final IdentityClient identityClient;

    @Override
    public Product createProduct(ProductRequest request, MultipartFile image, String token) {
        try {
            // 1. Lấy User ID từ Token (Logic tách biệt)
            String userId = extractUserIdFromToken(token);

            String imageUrl = request.getImageUrl();
            if (image != null && !image.isEmpty()) {
                imageUrl = cloudinaryService.uploadImage(image);
            }

            // 2. Map từ DTO sang Entity (Có thể dùng MapStruct sau này)
            Product product = Product.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .imageUrl(imageUrl)
                    .ownerId(userId) // Gán chủ sở hữu
                    .build();

            // 3. Lưu vào DB (cần id trước khi hash payload)
            Product saved = productRepository.save(product);
            return saved;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo mặt hàng catalog: " + e.getMessage());
        }
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getMyProducts(String token) {
        try {
            return productRepository.findAllByOwnerIdOrderByNameAsc(extractUserIdFromToken(token));
        } catch (Exception e) {
            throw new RuntimeException("Loi khi lay catalog cua nha san xuat: " + e.getMessage());
        }
    }

    @Override
    public Product getProductById(String id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Mặt hàng catalog không tồn tại"));
    }



    private String extractUserIdFromToken(String token) throws Exception {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}
