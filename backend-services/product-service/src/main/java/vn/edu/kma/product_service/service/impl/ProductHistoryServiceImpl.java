package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.edu.kma.product_service.dto.request.HistoryRequest;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.entity.ProductHistory;
import vn.edu.kma.product_service.repository.ProductHistoryRepository;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.service.ProductHistoryService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductHistoryServiceImpl implements ProductHistoryService {
    private final ProductHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.url.blockchain-service}")
    private String blockchainUrl;

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
            ProductHistory savedHistory = historyRepository.save(history);
            // --- GHI LÊN BLOCKCHAIN ---
            try {
                Map<String, String> blockchainRequest = new HashMap<>();
                blockchainRequest.put("productId", savedHistory.getProductId());
                blockchainRequest.put("action", savedHistory.getAction());
                blockchainRequest.put("description", savedHistory.getDescription());
                restTemplate.postForObject(
                        "http://localhost:8083/api/v1/blockchain/history",
                        blockchainRequest,
                        Object.class
                );
                log.info("Đã ghi lên Blockchain thành công!");
            } catch (Exception e) {
                // DB đã lưu rồi, Blockchain lỗi thì chỉ log warning
                // Không throw exception để tránh rollback DB
                log.warn("Lỗi ghi Blockchain (DB vẫn OK): {}", e.getMessage());
            }
            return savedHistory;

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
