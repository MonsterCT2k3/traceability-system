package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.kma.product_service.dto.request.ScanHistoryRequest;
import vn.edu.kma.product_service.dto.response.ScanHistoryResponse;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.entity.ProductUnit;
import vn.edu.kma.product_service.entity.ScanHistory;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.repository.ProductUnitRepository;
import vn.edu.kma.product_service.repository.ScanHistoryRepository;
import vn.edu.kma.product_service.service.ScanHistoryService;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScanHistoryServiceImpl implements ScanHistoryService {

    private final ScanHistoryRepository scanHistoryRepository;
    private final ProductUnitRepository productUnitRepository;
    private final ProductRepository productRepository;

    @Override
    public void recordScan(ScanHistoryRequest request, String tokenHeader) {
        try {
            String userId = extractUserIdFromToken(tokenHeader);
            String unitSerial = request.getUnitSerial();

            Optional<ScanHistory> existingOpt = scanHistoryRepository.findByUserIdAndUnitSerial(userId, unitSerial);

            if (existingOpt.isPresent()) {
                ScanHistory existing = existingOpt.get();
                existing.setScannedAt(LocalDateTime.now());
                scanHistoryRepository.save(existing);
            } else {
                ScanHistory newHistory = ScanHistory.builder()
                        .userId(userId)
                        .unitSerial(unitSerial)
                        .build();
                scanHistoryRepository.save(newHistory);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu lịch sử quét: " + e.getMessage());
        }
    }

    @Override
    public Page<ScanHistoryResponse> getScanHistory(String tokenHeader, int page, int size) {
        try {
            String userId = extractUserIdFromToken(tokenHeader);
            Pageable pageable = PageRequest.of(page, size);

            Page<ScanHistory> historyPage = scanHistoryRepository.findByUserIdOrderByScannedAtDesc(userId, pageable);

            return historyPage.map(history -> {
                String productName = "Unknown Product";
                String productImage = null;

                Optional<ProductUnit> unitOpt = productUnitRepository.findByUnitSerial(history.getUnitSerial());
                if (unitOpt.isPresent()) {
                    Optional<Product> productOpt = productRepository.findById(unitOpt.get().getProductId());
                    if (productOpt.isPresent()) {
                        productName = productOpt.get().getName();
                        productImage = productOpt.get().getImageUrl();
                    }
                }

                return ScanHistoryResponse.builder()
                        .id(history.getId())
                        .unitSerial(history.getUnitSerial())
                        .scannedAt(history.getScannedAt())
                        .productName(productName)
                        .productImage(productImage)
                        .build();
            });

        } catch (Exception e) {
            throw new RuntimeException("Lỗi lấy lịch sử quét: " + e.getMessage());
        }
    }

    private String extractUserIdFromToken(String token) throws Exception {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}
