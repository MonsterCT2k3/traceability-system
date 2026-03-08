package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.edu.kma.product_service.dto.request.ProductRequest;
import vn.edu.kma.product_service.dto.request.TransferRequest;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.entity.ProductHistory;
import vn.edu.kma.product_service.repository.ProductHistoryRepository;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.service.ProductService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductHistoryRepository historyRepository;
    private final RestTemplate restTemplate;



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

    @Override
    public Product transferOwnership(String productId, TransferRequest request, String token) {
        try{
            String currentUserId = extractUserIdFromToken(token);

            Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
            if(!product.getOwnerId().equals(currentUserId)){
                throw new RuntimeException("Bạn không có quyền thực hiện thao tác này");
            }

            if(product.getOwnerId().equals(request.getNewOwnerId())){
                throw new RuntimeException("Bạn không thể chuyển cho chính mình");
            }

            String previousOwnerId = product.getOwnerId();

            product.setOwnerId(request.getNewOwnerId());
            Product updatedProduct = productRepository.save(product);

            ProductHistory history = ProductHistory.builder()
                    .productId(productId)
                    .action("TRANSFER")
                    .description("Chuyển từ " + previousOwnerId + " sang " + request.getNewOwnerId())
                    .actorId(currentUserId)
                    .timestamp(LocalDateTime.now())
                    .build();
            historyRepository.save(history);

            // 8. Ghi lên Blockchain
            try {
                Map<String, String> blockchainRequest = new HashMap<>();
                blockchainRequest.put("productId", productId);
                blockchainRequest.put("action", "TRANSFER");
                blockchainRequest.put("description", "Chuyển từ " + previousOwnerId + " sang " + request.getNewOwnerId());
                restTemplate.postForObject(
                        "http://localhost:8083/api/v1/blockchain/history",
                        blockchainRequest,
                        Object.class
                );
                log.info("Đã ghi TRANSFER lên Blockchain!");
            } catch (Exception e) {
                log.warn("Lỗi ghi Blockchain (DB vẫn OK): {}", e.getMessage());
            }
            return updatedProduct;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
