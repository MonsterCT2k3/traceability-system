package vn.edu.kma.traceability_core_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.kma.traceability_core_service.client.CatalogClient;
import vn.edu.kma.traceability_core_service.service.IdentityClient;
import vn.edu.kma.traceability_core_service.dto.response.PackingUnitSerialItem;
import vn.edu.kma.traceability_core_service.dto.response.PalletBulkCartonResult;
import vn.edu.kma.traceability_core_service.dto.response.ProductPackingManifestResponse;
import vn.edu.kma.traceability_core_service.dto.response.ProductPackingSummaryResponse;
import vn.edu.kma.traceability_core_service.entity.Carton;
import vn.edu.kma.traceability_core_service.repository.CartonRepository;
import vn.edu.kma.traceability_core_service.repository.ProductUnitRepository;
import vn.edu.kma.traceability_core_service.repository.projection.ProductPackingSummaryProjection;
import vn.edu.kma.traceability_core_service.service.PackingService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PackingServiceImpl implements PackingService {
    private final CartonRepository cartonRepository;
    private final ProductUnitRepository productUnitRepository;
    private final IdentityClient identityClient;
    private final CatalogClient catalogClient;

    @Override
    public List<ProductPackingSummaryResponse> getMyPackingSummary(String tokenHeader) {
        try {
            String manufacturerId = extractUserIdFromToken(tokenHeader);
            List<ProductPackingSummaryProjection> rows = cartonRepository.summarizePackingByManufacturer(manufacturerId);
            return rows.stream()
                    .map(r -> ProductPackingSummaryResponse.builder()
                            .productId(r.getProductId())
                            .productName(resolveProductName(r.getProductId())) // Use CatalogClient
                            .cartonsCount(r.getCartonsCount())
                            .unitsCount(r.getUnitsCount())
                            .build())
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lấy thống kê sản xuất: " + e.getMessage());
        }
    }

    @Override
    public List<ProductPackingManifestResponse> getMyPackingManifest(String tokenHeader) {
        try {
            String manufacturerId = extractUserIdFromToken(tokenHeader);
            List<Carton> cartons = cartonRepository.findByManufacturerIdWithFallback(manufacturerId);
            Map<String, ProductPackingManifestResponse> grouped = new LinkedHashMap<>();

            Map<String, String> ownerNameCache = new java.util.HashMap<>();
            for (Carton carton : cartons) {
                List<PackingUnitSerialItem> unitSerials = productUnitRepository.findByCartonIdOrderByCreatedAtAsc(carton.getId()).stream()
                        .map(u -> PackingUnitSerialItem.builder().unitSerial(u.getUnitSerial()).build())
                        .toList();

                ProductPackingManifestResponse current = grouped.computeIfAbsent(carton.getProductId(), id ->
                        ProductPackingManifestResponse.builder()
                                .productId(id)
                                .productName(resolveProductName(id))
                                .cartons(new java.util.ArrayList<>())
                                .build()
                );

                String status = (carton.getStatus() != null) ? carton.getStatus() : "IN_STOCK";
                String ownerName = ownerNameCache.computeIfAbsent(carton.getOwnerId(), this::fetchOwnerName);

                current.getCartons().add(PalletBulkCartonResult.builder()
                        .cartonCode(carton.getCartonCode())
                        .ownerId(carton.getOwnerId())
                        .ownerName(ownerName)
                        .status(status)
                        .units(unitSerials)
                        .build());
            }
            return new java.util.ArrayList<>(grouped.values());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lấy manifest đóng gói: " + e.getMessage());
        }
    }

    private String fetchOwnerName(String actorId) {
        if (actorId == null || actorId.isBlank()) return "Unknown";
        try {
            vn.edu.kma.common.dto.response.ApiResponse<java.util.Map<String, Object>> response = identityClient.getUserById(actorId);
            if (response != null && response.getResult() != null) {
                java.util.Map<String, Object> result = response.getResult();
                Object fullNameObj = result.get("fullName");
                if (fullNameObj != null && !fullNameObj.toString().isBlank()) {
                    return fullNameObj.toString();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "Unknown";
    }

    private String resolveProductName(String productId) {
        try {
            vn.edu.kma.common.dto.response.ApiResponse<java.util.Map<String, Object>> response = catalogClient.getProductById(productId);
            if (response != null && response.getResult() != null) {
                java.util.Map<String, Object> result = response.getResult();
                Object nameObj = result.get("name");
                if (nameObj != null && !nameObj.toString().isBlank()) {
                    return nameObj.toString();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return productId;
    }

    private String extractUserIdFromToken(String token) throws Exception {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}

