package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.kma.product_service.dto.request.CartonCreateRequest;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.entity.Pallet;
import vn.edu.kma.product_service.repository.CartonRepository;
import vn.edu.kma.product_service.repository.PalletRepository;
import vn.edu.kma.product_service.service.CartonService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartonServiceImpl implements CartonService {

    private final PalletRepository palletRepository;
    private final CartonRepository cartonRepository;

    @Override
    public Carton createCarton(String palletId, CartonCreateRequest request, String tokenHeader) {
        try {
            String userId = extractUserIdFromToken(tokenHeader);
            if (request.getCartonCode() == null || request.getCartonCode().isBlank()) {
                throw new RuntimeException("cartonCode là bắt buộc");
            }
            if (request.getPlannedUnitCount() == null || request.getPlannedUnitCount() <= 0) {
                throw new RuntimeException("plannedUnitCount phải là số dương");
            }

            Pallet pallet = palletRepository.findById(palletId)
                    .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));
            if (!pallet.getOwnerId().equals(userId)) {
                throw new RuntimeException("Chỉ owner hiện tại của pallet mới được tạo carton");
            }

            String code = request.getCartonCode().trim();
            cartonRepository.findByCartonCode(code).ifPresent(c -> {
                throw new RuntimeException("cartonCode đã tồn tại");
            });

            Carton saved = cartonRepository.save(Carton.builder()
                    .palletId(pallet.getId())
                    .productId(pallet.getProductId())
                    .cartonCode(code)
                    .plannedUnitCount(request.getPlannedUnitCount())
                    .unitLabel(trimToNull(request.getUnitLabel()))
                    .ownerId(pallet.getOwnerId())
                    .note(trimToNull(request.getNote()))
                    .build());
            return saved;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("createCarton failed", e);
            throw new RuntimeException("Lỗi tạo carton: " + e.getMessage());
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String extractUserIdFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }
}
