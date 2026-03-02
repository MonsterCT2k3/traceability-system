package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.kma.product_service.dto.request.HistoryRequest;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.entity.ProductHistory;
import vn.edu.kma.product_service.repository.ProductHistoryRepository;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.service.ProductHistoryService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductHistoryServiceImpl implements ProductHistoryService {
    private final ProductHistoryRepository historyRepository;
    private final ProductRepository productRepository;

    @Override
    public ProductHistory createHistory(HistoryRequest request, String token) {
        try {
            // 1. Lấy User ID từ Token (Logic tách biệt)
            String userId = extractUserIdFromToken(token);

            // 2. Kiểm tra sản phẩm tồn tại
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            // 3. Kiểm tra quyền sở hữu (Logic quan trọng)
            if (!product.getOwnerId().equals(userId)) {
                throw new RuntimeException("Bạn không có quyền ghi nhật ký cho sản phẩm này!");
            }

            // 4. Tạo Entity từ Request
            ProductHistory history = ProductHistory.builder()
                    .productId(request.getProductId())
                    .action(request.getAction())
                    .description(request.getDescription())
                    .location(request.getLocation())
                    .actorId(userId)
                    .timestamp(LocalDateTime.now())
                    .build();

            // 5. Lưu vào DB
            return historyRepository.save(history);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi ghi nhật ký: " + e.getMessage());
        }
    }

    @Override
    public List<ProductHistory> getHistoryByProductId(String productId) {
        return historyRepository.findByProductIdOrderByTimestampDesc(productId);
    }

    // Hàm tiện ích private để tách logic xử lý token
    private String extractUserIdFromToken(String token) throws Exception {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}
