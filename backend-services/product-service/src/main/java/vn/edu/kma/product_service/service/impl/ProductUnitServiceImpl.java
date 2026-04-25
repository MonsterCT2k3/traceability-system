package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.product_service.dto.request.ProductUnitGenerateRequest;
import vn.edu.kma.product_service.dto.response.ProductUnitGeneratedItem;
import vn.edu.kma.product_service.dto.response.ProductUnitGenerateResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitPublicTraceResponse;
import vn.edu.kma.product_service.entity.Carton;
import vn.edu.kma.product_service.entity.Pallet;
import vn.edu.kma.product_service.entity.Product;
import vn.edu.kma.product_service.entity.ProductUnit;
import vn.edu.kma.product_service.repository.BanknoteSerialRepository;
import vn.edu.kma.product_service.repository.CartonRepository;
import vn.edu.kma.product_service.repository.PalletRepository;
import vn.edu.kma.product_service.repository.ProductRepository;
import vn.edu.kma.product_service.repository.ProductUnitRepository;
import vn.edu.kma.product_service.service.ProductUnitService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductUnitServiceImpl implements ProductUnitService {

    private final CartonRepository cartonRepository;
    private final ProductUnitRepository productUnitRepository;
    private final PalletRepository palletRepository;
    private final ProductRepository productRepository;
    private final BanknoteSerialRepository banknoteSerialRepository;

    @Override
    @Transactional
    public ProductUnitGenerateResponse generateUnits(String cartonId, ProductUnitGenerateRequest request, String tokenHeader) {
        try {
            String userId = extractUserIdFromToken(tokenHeader);
            Carton carton = cartonRepository.findById(cartonId)
                    .orElseThrow(() -> new RuntimeException("Carton không tồn tại"));
            if (!carton.getOwnerId().equals(userId)) {
                throw new RuntimeException("Chỉ owner carton mới được sinh unit");
            }

            List<String> requestedSerials = normalizeRequestedSerials(request.getSerials());
            boolean useProvidedSerials = !requestedSerials.isEmpty();
            int count;
            if (useProvidedSerials) {
                count = requestedSerials.size();
            } else {
                if (request.getCount() == null || request.getCount() <= 0) {
                    throw new RuntimeException("count phải là số dương nếu không truyền serials");
                }
                count = request.getCount();
            }

            long existing = productUnitRepository.countByCartonId(cartonId);
            if (existing + count > carton.getPlannedUnitCount()) {
                throw new RuntimeException(
                        "Vượt quá plannedUnitCount của carton (đã có " + existing + ", thêm " + count + ", tối đa "
                                + carton.getPlannedUnitCount() + ")");
            }

            if (useProvidedSerials) {
                validateBanknoteSerialOwnership(requestedSerials, userId);
            }

            long baseSeq = existing + 1;
            List<ProductUnitGeneratedItem> items = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                String unitSerial;
                if (useProvidedSerials) {
                    unitSerial = requestedSerials.get(i);
                } else {
                    long seq = baseSeq + i;
                    if (seq > 9999L) {
                        throw new RuntimeException("Số thứ tự unit trong carton vượt quá 9999");
                    }
                    unitSerial = String.format("%s-U%04d", carton.getCartonCode(), seq);
                }

                if (productUnitRepository.findByUnitSerial(unitSerial).isPresent()) {
                    throw new RuntimeException("unitSerial đã tồn tại: " + unitSerial);
                }

                ProductUnit saved = productUnitRepository.save(ProductUnit.builder()
                        .cartonId(carton.getId())
                        .palletId(carton.getPalletId())
                        .productId(carton.getProductId())
                        .unitSerial(unitSerial)
                        .build());

                items.add(ProductUnitGeneratedItem.builder()
                        .unitId(saved.getId())
                        .unitSerial(saved.getUnitSerial())
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

    private static List<String> normalizeRequestedSerials(List<String> serials) {
        if (serials == null || serials.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String raw : serials) {
            if (raw == null || raw.isBlank()) {
                throw new RuntimeException("Danh sách serials chứa phần tử rỗng");
            }
            String norm = raw.trim().toUpperCase(Locale.ROOT);
            if (!norm.matches("^[A-Z0-9\\-]{4,32}$")) {
                throw new RuntimeException("Serial không hợp lệ: " + raw);
            }
            if (!out.add(norm)) {
                throw new RuntimeException("Serial bị trùng trong request: " + norm);
            }
        }
        return new ArrayList<>(out);
    }

    private void validateBanknoteSerialOwnership(List<String> serials, String userId) {
        Set<String> mine = banknoteSerialRepository
                .findBySerialValueInAndRegisteredByUserId(serials, userId)
                .stream()
                .map(b -> b.getSerialValue().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        for (String s : serials) {
            if (!mine.contains(s)) {
                throw new RuntimeException("Serial không thuộc kho seri của bạn hoặc chưa đăng ký: " + s);
            }
        }
    }

    @Override
    @Transactional
    public ProductUnitPublicTraceResponse getPublicTraceByUnitId(String unitId) {
        ProductUnit unit = productUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit không tồn tại"));
        unit.setScanCount((unit.getScanCount() == null ? 0 : unit.getScanCount()) + 1);
        productUnitRepository.save(unit);
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
        unit.setScanCount((unit.getScanCount() == null ? 0 : unit.getScanCount()) + 1);
        productUnitRepository.save(unit);
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
                .scanCount(scanDisplay)
                .build();
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
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
