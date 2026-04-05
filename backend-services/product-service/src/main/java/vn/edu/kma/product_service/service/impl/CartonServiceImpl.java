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
            if (request.getPlannedUnitCount() == null || request.getPlannedUnitCount() <= 0) {
                throw new RuntimeException("plannedUnitCount phải là số dương");
            }

            Pallet pallet = palletRepository.findById(palletId)
                    .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));
            if (!pallet.getOwnerId().equals(userId)) {
                throw new RuntimeException("Chỉ owner hiện tại của pallet mới được tạo carton");
            }

            String code = resolveCartonCode(trimToNull(request.getCartonCode()), pallet);
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

    /**
     * Tự sinh: CTN-{palletCode}-{seq 4 số}, seq tăng theo số carton hiện có của pallet (có retry trùng).
     */
    private String resolveCartonCode(String userProvided, Pallet pallet) {
        if (userProvided != null) {
            return userProvided;
        }
        String pc = pallet.getPalletCode().trim();
        long baseSeq = cartonRepository.countByPalletId(pallet.getId()) + 1;
        for (int offset = 0; offset < 10_000; offset++) {
            long seq = baseSeq + offset;
            if (seq > 9999L) {
                throw new RuntimeException("Đã vượt quá 9999 carton trên một pallet");
            }
            String candidate = String.format("CTN-%s-%04d", pc, seq);
            if (cartonRepository.findByCartonCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new RuntimeException("Không sinh được cartonCode duy nhất, thử lại");
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
