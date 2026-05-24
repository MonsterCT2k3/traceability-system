package vn.edu.kma.traceability_core_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.kma.traceability_core_service.dto.request.ScanHistoryRequest;
import vn.edu.kma.traceability_core_service.dto.response.ScanHistoryResponse;
import vn.edu.kma.traceability_core_service.entity.ProductUnit;
import vn.edu.kma.traceability_core_service.entity.ScanHistory;
import vn.edu.kma.traceability_core_service.client.CatalogClient;
import vn.edu.kma.traceability_core_service.repository.ProductUnitRepository;
import vn.edu.kma.traceability_core_service.repository.ScanHistoryRepository;
import vn.edu.kma.traceability_core_service.service.ScanHistoryService;

import java.time.LocalDateTime;
import java.util.Optional;
import vn.edu.kma.common.dto.response.ApiResponse;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScanHistoryServiceImpl implements ScanHistoryService {

    private final ScanHistoryRepository scanHistoryRepository;
    private final ProductUnitRepository productUnitRepository;
    private final CatalogClient catalogClient;

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
                ProductUnit productUnit = productUnitRepository.findByUnitSerial(unitSerial)
                        .orElseThrow(() -> new RuntimeException("Unit không tồn tại: " + unitSerial));
                ScanHistory newHistory = ScanHistory.builder()
                        .userId(userId)
                        .productUnit(productUnit)
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

                ProductUnit unit = history.getProductUnit();
                if (unit != null && unit.getProductId() != null) {
                    try {
                        ApiResponse<Map<String, Object>> pRes = catalogClient.getProductById(unit.getProductId());
                        if (pRes != null && pRes.getResult() != null) {
                            Map<String, Object> pMap = pRes.getResult();
                            productName = (String) pMap.get("name");
                            productImage = (String) pMap.get("imageUrl");
                        }
                    } catch(Exception ignored) {}
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

