package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.product_service.dto.request.ProductUnitClaimRequest;
import vn.edu.kma.product_service.dto.request.ProductUnitGenerateRequest;
import vn.edu.kma.product_service.dto.request.ProductUnitSecretScanRequest;
import vn.edu.kma.product_service.dto.response.ProductUnitClaimResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitGeneratedItem;
import vn.edu.kma.product_service.dto.response.ProductUnitGenerateResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitPublicTraceResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitSecretScanResponse;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.entity.Pallet;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.entity.ProductUnit;
import vn.edu.kma.product_service.repository.CartonRepository;
import vn.edu.kma.product_service.repository.PalletRepository;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.repository.ProductUnitRepository;
import vn.edu.kma.product_service.service.ProductUnitService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductUnitServiceImpl implements ProductUnitService {

    private static final String SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SECRET_LENGTH = 12;

    private final CartonRepository cartonRepository;
    private final ProductUnitRepository productUnitRepository;
    private final PalletRepository palletRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductUnitGenerateResponse generateUnits(String cartonId, ProductUnitGenerateRequest request, String tokenHeader) {
        try {
            String userId = extractUserIdFromToken(tokenHeader);
            if (request.getCount() == null || request.getCount() <= 0) {
                throw new RuntimeException("count phải là số dương");
            }
            int count = request.getCount();

            Carton carton = cartonRepository.findById(cartonId)
                    .orElseThrow(() -> new RuntimeException("Carton không tồn tại"));
            if (!carton.getOwnerId().equals(userId)) {
                throw new RuntimeException("Chỉ owner carton mới được sinh unit");
            }

            long existing = productUnitRepository.countByCartonId(cartonId);
            if (existing + count > carton.getPlannedUnitCount()) {
                throw new RuntimeException(
                        "Vượt quá plannedUnitCount của carton (đã có " + existing + ", thêm " + count + ", tối đa "
                                + carton.getPlannedUnitCount() + ")");
            }

            long baseSeq = existing + 1;
            List<ProductUnitGeneratedItem> items = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                long seq = baseSeq + i;
                if (seq > 9999L) {
                    throw new RuntimeException("Số thứ tự unit trong carton vượt quá 9999");
                }
                String unitSerial = String.format("%s-U%04d", carton.getCartonCode(), seq);

                if (productUnitRepository.findByUnitSerial(unitSerial).isPresent()) {
                    throw new RuntimeException("Trùng unitSerial (không mong đợi): " + unitSerial);
                }

                String secretPlain = randomSecretPlain();
                String secretHash = sha256HexUtf8(secretPlain);
                int guard = 0;
                while (productUnitRepository.findBySecretHash(secretHash).isPresent() && guard++ < 20) {
                    secretPlain = randomSecretPlain();
                    secretHash = sha256HexUtf8(secretPlain);
                }
                if (productUnitRepository.findBySecretHash(secretHash).isPresent()) {
                    throw new RuntimeException("Không sinh được secret hash duy nhất");
                }

                ProductUnit saved = productUnitRepository.save(ProductUnit.builder()
                        .cartonId(carton.getId())
                        .palletId(carton.getPalletId())
                        .productId(carton.getProductId())
                        .unitSerial(unitSerial)
                        .secretHash(secretHash)
                        .build());

                items.add(ProductUnitGeneratedItem.builder()
                        .unitId(saved.getId())
                        .unitSerial(saved.getUnitSerial())
                        .secretPlain(secretPlain)
                        .build());
            }

            return ProductUnitGenerateResponse.builder()
                    .cartonId(carton.getId())
                    .cartonCode(carton.getCartonCode())
                    .units(items)
                    .build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("generateUnits failed", e);
            throw new RuntimeException("Lỗi sinh unit: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ProductUnitClaimResponse claimUnit(String unitId, ProductUnitClaimRequest request, String tokenHeader) {
        try {
            String userId = extractUserIdFromToken(tokenHeader);
            if (request.getSecretPlain() == null || request.getSecretPlain().isBlank()) {
                throw new RuntimeException("secretPlain là bắt buộc");
            }
            String plain = request.getSecretPlain().trim();

            ProductUnit unit = productUnitRepository.findById(unitId)
                    .orElseThrow(() -> new RuntimeException("Unit không tồn tại"));
            if (unit.getSecretHash() == null || unit.getSecretHash().isBlank()) {
                throw new RuntimeException("Unit này không có secret scratch-off");
            }
            if (unit.getOwnerId() != null) {
                throw new RuntimeException("Unit đã được claim, không thể claim lại");
            }

            String computed = sha256HexUtf8(plain);
            if (!MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.US_ASCII),
                    unit.getSecretHash().getBytes(StandardCharsets.US_ASCII))) {
                throw new RuntimeException("Secret không đúng");
            }

            String nameSnapshot = extractOptionalFullNameFromToken(tokenHeader);
            LocalDateTime now = LocalDateTime.now();
            unit.setOwnerId(userId);
            unit.setClaimedAt(now);
            unit.setOwnerNameSnapshot(nameSnapshot);
            productUnitRepository.save(unit);

            return ProductUnitClaimResponse.builder()
                    .unitId(unit.getId())
                    .unitSerial(unit.getUnitSerial())
                    .ownerId(unit.getOwnerId())
                    .claimedAt(unit.getClaimedAt())
                    .ownerNameSnapshot(unit.getOwnerNameSnapshot())
                    .build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("claimUnit failed", e);
            throw new RuntimeException("Lỗi claim: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ProductUnitSecretScanResponse recordSecretScan(String unitId, ProductUnitSecretScanRequest request) {
        if (request.getSecretPlain() == null || request.getSecretPlain().isBlank()) {
            throw new RuntimeException("secretPlain là bắt buộc");
        }
        String plain = request.getSecretPlain().trim();

        ProductUnit unit = productUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit không tồn tại"));
        if (unit.getSecretHash() == null || unit.getSecretHash().isBlank()) {
            throw new RuntimeException("Unit này không có secret scratch-off");
        }

        String computed = sha256HexUtf8(plain);
        if (!MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.US_ASCII),
                unit.getSecretHash().getBytes(StandardCharsets.US_ASCII))) {
            throw new RuntimeException("Secret không đúng");
        }

        int nextScan = unit.getScanCount() == null ? 1 : unit.getScanCount() + 1;
        unit.setScanCount(nextScan);
        productUnitRepository.save(unit);

        return ProductUnitSecretScanResponse.builder()
                .unitId(unit.getId())
                .unitSerial(unit.getUnitSerial())
                .scanCount(nextScan)
                .build();
    }

    @Override
    @Transactional
    public ProductUnitPublicTraceResponse getPublicTraceByUnitId(String unitId) {
        ProductUnit unit = productUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit không tồn tại"));
        return buildPublicTrace(unit);
    }

    @Override
    @Transactional
    public ProductUnitPublicTraceResponse getPublicTraceByUnitSerial(String unitSerial) {
        if (unitSerial == null || unitSerial.isBlank()) {
            throw new RuntimeException("unitSerial là bắt buộc");
        }
        ProductUnit unit = productUnitRepository.findByUnitSerial(unitSerial.trim())
                .orElseThrow(() -> new RuntimeException("Unit không tồn tại"));
        return buildPublicTrace(unit);
    }

    @Override
    public String getTraceQrPayload(String unitId) {
        if (!productUnitRepository.existsById(unitId)) {
            throw new RuntimeException("Unit không tồn tại");
        }
        return unitId;
    }

    private ProductUnitPublicTraceResponse buildPublicTrace(ProductUnit unit) {
        Carton carton = cartonRepository.findById(unit.getCartonId())
                .orElseThrow(() -> new RuntimeException("Carton không tồn tại"));
        Pallet pallet = palletRepository.findById(unit.getPalletId())
                .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));
        Product product = productRepository.findById(unit.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm catalog không tồn tại"));

        int scanDisplay = unit.getScanCount() == null ? 0 : unit.getScanCount();

        return ProductUnitPublicTraceResponse.builder()
                .unitId(unit.getId())
                .unitSerial(unit.getUnitSerial())
                .productId(product.getId())
                .productName(product.getName())
                .productDescription(product.getDescription())
                .productImageUrl(product.getImageUrl())
                .cartonCode(carton.getCartonCode())
                .palletCode(pallet.getPalletCode())
                .palletName(pallet.getPalletName())
                .palletManufacturedAt(pallet.getManufacturedAt())
                .palletExpiryAt(emptyToNull(pallet.getExpiryAt()))
                .claimed(unit.getOwnerId() != null && !unit.getOwnerId().isBlank())
                .scanCount(scanDisplay)
                .build();
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String randomSecretPlain() {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(SECRET_LENGTH);
        for (int i = 0; i < SECRET_LENGTH; i++) {
            sb.append(SECRET_ALPHABET.charAt(r.nextInt(SECRET_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String sha256HexUtf8(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256", e);
        }
    }

    private static String extractUserIdFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }

    /**
     * JWT hiện tại có thể chưa có claim này; khi identity thêm fullName vào token sẽ tự lưu snapshot.
     */
    private static String extractOptionalFullNameFromToken(String tokenHeader) {
        try {
            String token = tokenHeader;
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            SignedJWT signedJWT = SignedJWT.parse(token);
            String name = signedJWT.getJWTClaimsSet().getStringClaim("fullName");
            if (name == null || name.isBlank()) {
                return null;
            }
            return name.trim();
        } catch (Exception e) {
            return null;
        }
    }
}
