package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
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
import vn.edu.kma.product_service.repository.RawBatchRepository;
import vn.edu.kma.product_service.repository.TransferRecordRepository;
import vn.edu.kma.product_service.service.ProductUnitService;
import vn.edu.kma.product_service.dto.response.TraceHistoryEvent;
import vn.edu.kma.product_service.entity.RawBatch;
import vn.edu.kma.product_service.entity.TransferRecord;

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
    private final RawBatchRepository rawBatchRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final RestTemplate restTemplate;

    @Value("${app.services.identity.url:http://localhost:8081}")
    private String identityServiceUrl;

    @Override
    @Transactional
    public ProductUnitGenerateResponse generateUnits(String cartonId, ProductUnitGenerateRequest request,
            String tokenHeader) {
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
                        .ownerId(userId)
                        .manufacturerId(carton.getManufacturerId())
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

        List<TraceHistoryEvent> historyEvents = buildHistoryEvents(pallet, carton, unit);

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
                .historyEvents(historyEvents)
                .build();
    }

    private List<TraceHistoryEvent> buildHistoryEvents(Pallet pallet, Carton carton, ProductUnit unit) {
        List<TraceHistoryEvent> events = new ArrayList<>();

        // 1. Tìm thông tin Lô nguyên liệu gốc (RawBatch)
        if (pallet.getParentRawBatchIdHexes() != null && !pallet.getParentRawBatchIdHexes().isBlank()) {
            String[] rawBatchHexes = pallet.getParentRawBatchIdHexes().split(",");
            for (String hex : rawBatchHexes) {
                rawBatchRepository.findByBatchIdHex(hex.trim()).ifPresent(raw -> {
                    events.add(TraceHistoryEvent.builder()
                            .eventType("RAW_BATCH_CREATED")
                            .eventDescription("Khai báo lô nguyên liệu gốc: " + raw.getMaterialName())
                            .timestamp(raw.getCreatedAt())
                            .actorId(raw.getOwnerId())
                            .actorName(fetchActorName(raw.getOwnerId()))
                            .location(raw.getLocation())
                            .txHash(raw.getBatchIdHex())
                            .build());

                    // Tìm lịch sử vận chuyển của RawBatch này (nếu có)
                    List<TransferRecord> rawTransfers = transferRecordRepository
                            .findByTargetTypeAndTargetIdOrderByCreatedAtAsc("RAW_BATCH", raw.getId());
                    for (TransferRecord tr : rawTransfers) {
                        if ("ACCEPTED".equals(tr.getStatus())) {
                            events.add(TraceHistoryEvent.builder()
                                    .eventType("RAW_BATCH_TRANSFERRED")
                                    .eventDescription("Chuyển giao nguyên liệu thành công")
                                    .timestamp(tr.getUpdatedAt() != null ? tr.getUpdatedAt() : tr.getCreatedAt())
                                    .actorId(tr.getToUserId()) // Người nhận
                                    .actorName(fetchActorName(tr.getToUserId()))
                                    .txHash(tr.getBlockchainTxHash())
                                    .build());
                        }
                    }
                });
            }
        }

        // 2. Sự kiện sản xuất Pallet
        events.add(TraceHistoryEvent.builder()
                .eventType("PALLET_MANUFACTURED")
                .eventDescription("Sản xuất và đóng gói lô hàng: " + pallet.getPalletName())
                .timestamp(pallet.getCreatedAt())
                .actorId(pallet.getOwnerId())
                .actorName(fetchActorName(pallet.getOwnerId()))
                .location(pallet.getLocation())
                .txHash(pallet.getChainBatchIdHex())
                .build());

        // 3. Sự kiện chuyển giao Pallet
        List<TransferRecord> palletTransfers = transferRecordRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtAsc("PALLET", pallet.getId());
        for (TransferRecord tr : palletTransfers) {
            if ("ACCEPTED".equals(tr.getStatus())) {
                events.add(TraceHistoryEvent.builder()
                        .eventType("PALLET_TRANSFERRED")
                        .eventDescription("Lô hàng được chuyển giao thành công")
                        .timestamp(tr.getUpdatedAt() != null ? tr.getUpdatedAt() : tr.getCreatedAt())
                        .actorId(tr.getToUserId())
                        .actorName(fetchActorName(tr.getToUserId()))
                        .txHash(tr.getBlockchainTxHash())
                        .build());
            }
        }

        // 4. Sự kiện chuyển giao Thùng (Carton)
        List<TransferRecord> cartonTransfers = transferRecordRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtAsc("CARTON", carton.getId());
        for (TransferRecord tr : cartonTransfers) {
            if ("ACCEPTED".equals(tr.getStatus())) {
                events.add(TraceHistoryEvent.builder()
                        .eventType("CARTON_TRANSFERRED")
                        .eventDescription("Thùng hàng được chuyển giao thành công")
                        .timestamp(tr.getUpdatedAt() != null ? tr.getUpdatedAt() : tr.getCreatedAt())
                        .actorId(tr.getToUserId())
                        .actorName(fetchActorName(tr.getToUserId()))
                        .txHash(tr.getBlockchainTxHash())
                        .build());
            }
        }

        // 5. Sự kiện chuyển giao Đơn vị sản phẩm (Unit)
        List<TransferRecord> unitTransfers = transferRecordRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtAsc("UNIT", unit.getId());
        for (TransferRecord tr : unitTransfers) {
            if ("ACCEPTED".equals(tr.getStatus())) {
                events.add(TraceHistoryEvent.builder()
                        .eventType("UNIT_TRANSFERRED")
                        .eventDescription("Sản phẩm được chuyển giao thành công")
                        .timestamp(tr.getUpdatedAt() != null ? tr.getUpdatedAt() : tr.getCreatedAt())
                        .actorId(tr.getToUserId())
                        .actorName(fetchActorName(tr.getToUserId()))
                        .txHash(tr.getBlockchainTxHash())
                        .build());
            }
        }

        // Sắp xếp theo thời gian tăng dần (từ gốc tới hiện tại)
        events.sort((a, b) -> {
            if (a.getTimestamp() == null)
                return -1;
            if (b.getTimestamp() == null)
                return 1;
            return a.getTimestamp().compareTo(b.getTimestamp());
        });

        return events;
    }

    private String fetchActorName(String actorId) {
        if (actorId == null || actorId.isBlank())
            return null;
        try {
            String url = identityServiceUrl + "/api/v1/users/directory/by-id/" + actorId;
            vn.edu.kma.common.dto.response.ApiResponse<?> response = restTemplate.getForObject(url,
                    vn.edu.kma.common.dto.response.ApiResponse.class);
            if (response != null && response.getResult() != null) {
                java.util.Map<?, ?> result = (java.util.Map<?, ?>) response.getResult();
                Object fullNameObj = result.get("fullName");
                if (fullNameObj != null && !fullNameObj.toString().isBlank()) {
                    return fullNameObj.toString();
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch actor name for id: {}", actorId);
        }
        return null;
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
