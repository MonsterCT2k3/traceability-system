package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.ProductUnitGenerateRequest;
import vn.edu.kma.product_service.dto.request.VerifyHashesRequest;
import vn.edu.kma.product_service.dto.response.ProductUnitGenerateResponse;
import vn.edu.kma.product_service.dto.response.ProductUnitGeneratedItem;
import vn.edu.kma.product_service.dto.response.ProductUnitPublicTraceResponse;
import vn.edu.kma.product_service.dto.response.TraceHistoryEvent;
import vn.edu.kma.product_service.dto.response.VerifyHashesResponse;
import vn.edu.kma.product_service.entity.*;
import vn.edu.kma.product_service.repository.*;
import vn.edu.kma.product_service.service.ProductUnitService;
import vn.edu.kma.product_service.utils.BlockchainVerificationUtils;

import java.util.*;

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

    @Value("${spring.url.blockchain-service}")
    private String blockchainBaseUrl;

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
        return buildPublicTrace(unit, false);
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
        return buildPublicTrace(unit, false);
    }

    @Override
    public ProductUnitPublicTraceResponse verifyPublicTrace(String unitSerial) {
        if (unitSerial == null || unitSerial.isBlank()) {
            throw new RuntimeException("unitSerial là bắt buộc");
        }
        ProductUnit unit = productUnitRepository.findByUnitSerial(unitSerial.trim())
                .orElseThrow(() -> new RuntimeException("Unit không tồn tại"));
        
        // BƯỚC 1: BẮT ĐẦU XÁC THỰC
        // Khi gọi API verify, ta không tăng biến đếm số lượt quét (scanCount) 
        // để tránh làm sai lệch số liệu thực tế khi người dùng chỉ muốn kiểm tra lại dữ liệu.
        // Truyền cờ verify = true để kích hoạt logic đối chiếu với Blockchain ở phần dưới.
        return buildPublicTrace(unit, true);
    }

    @Override
    public String getTraceQrPayload(String unitId) {
        if (!productUnitRepository.existsById(unitId)) {
            throw new RuntimeException("Unit không tồn tại");
        }
        return unitId;
    }

    private ProductUnitPublicTraceResponse buildPublicTrace(ProductUnit unit, boolean verify) {
        Carton carton = cartonRepository.findById(unit.getCartonId())
                .orElseThrow(() -> new RuntimeException("Carton không tồn tại"));
        Pallet pallet = palletRepository.findById(unit.getPalletId())
                .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));
        Product product = productRepository.findById(unit.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm catalog không tồn tại"));

        int scanDisplay = unit.getScanCount() == null ? 0 : unit.getScanCount();

        // BƯỚC 2: TRUY VẤN VÀ GOM DỮ LIỆU LỊCH SỬ TỪ DATABASE
        // Hệ thống sẽ lội ngược dòng từ Sản phẩm lẻ (Unit) -> Thùng (Carton) -> Lô (Pallet) -> Nguyên liệu gốc (RawBatch)
        // và thu thập tất cả các sự kiện vận chuyển tương ứng để tạo thành dòng thời gian (historyEvents).
        List<TraceHistoryEvent> historyEvents = buildHistoryEvents(pallet, carton, unit, verify);

        // BƯỚC 4: KẾT LUẬN TOÀN VẸN DỮ LIỆU
        // Đánh giá xem dữ liệu trên hệ thống có bị sửa đổi trái phép hay không
        Boolean isDataIntact = null;
        if (verify) {
            isDataIntact = true; // Mặc định tin tưởng là dữ liệu nguyên vẹn (true)
            for (TraceHistoryEvent event : historyEvents) {
                // Kiểm tra từng sự kiện trong lịch sử xem kết quả đối chiếu Blockchain là gì
                // Nếu là sự kiện quan trọng (Tạo Nguyên liệu hoặc Sản xuất Pallet) mà kết quả là false (Hash không khớp)
                // -> Nghĩa là dữ liệu trong Database (tên, số lượng,...) đã bị ai đó sửa đổi, không còn giống như lúc neo lên Blockchain.
                if (event.getIsVerifiedOnChain() != null && !event.getIsVerifiedOnChain()) {
                    if ("RAW_BATCH_CREATED".equals(event.getEventType()) || "PALLET_MANUFACTURED".equals(event.getEventType())) {
                        isDataIntact = false; // Báo động: Chuỗi dữ liệu đã bị can thiệp!
                        break; // Chỉ cần 1 mắt xích sai là toàn bộ dữ liệu bị coi là không đáng tin cậy
                    }
                }
            }
        }

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
                .isDataIntact(isDataIntact)
                .build();
    }

    private List<TraceHistoryEvent> buildHistoryEvents(Pallet pallet, Carton carton, ProductUnit unit, boolean verify) {
        List<TraceHistoryEvent> events = new ArrayList<>();
        List<RawBatch> rawBatches = new ArrayList<>();

        // 1. Tìm thông tin Lô nguyên liệu gốc (RawBatch)
        if (pallet.getParentRawBatchIdHexes() != null && !pallet.getParentRawBatchIdHexes().isBlank()) {
            String[] rawBatchHexes = pallet.getParentRawBatchIdHexes().split(",");
            for (String hex : rawBatchHexes) {
                rawBatchRepository.findByBatchIdHex(hex.trim()).ifPresent(raw -> {
                    rawBatches.add(raw);
                    events.add(TraceHistoryEvent.builder()
                            .eventType("RAW_BATCH_CREATED")
                            .eventDescription("Khai báo lô nguyên liệu gốc: " + raw.getMaterialName())
                            .timestamp(raw.getCreatedAt())
                            .actorId(raw.getActorId())
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

        // Populate actor details (name, avatar, and fallback location)
        for (TraceHistoryEvent event : events) {
            if (event.getActorId() != null) {
                ActorDetails details = fetchActorDetails(event.getActorId());
                event.setActorName(details.name());
                event.setActorAvatarUrl(details.avatarUrl());
                // Fallback location to actor's location if the event itself doesn't have one (e.g., Transfer events)
                if ((event.getLocation() == null || event.getLocation().isBlank()) && details.location() != null) {
                    event.setLocation(details.location());
                }
            }
        }

        // 6. Thực hiện xác thực nếu yêu cầu
        if (verify) {
            performBlockchainVerification(events, pallet, rawBatches);
        }

        return events;
    }

    // BƯỚC 3: ĐỐI CHIẾU HASH VỚI BLOCKCHAIN
    // Hàm này chỉ được chạy khi cờ verify = true
    private void performBlockchainVerification(List<TraceHistoryEvent> events, Pallet pallet, List<RawBatch> rawBatches) {
        try {
            List<VerifyHashesRequest.HashItem> items = new ArrayList<>();
            
            // 3.1. Tính toán lại mã băm (Hash) của Lô hàng (Pallet) DỰA TRÊN DỮ LIỆU HIỆN CÓ TRONG DB
            // Nếu ai đó sửa DB, hàm calculatePalletHash sẽ ra một chuỗi Hash khác hoàn toàn so với Hash gốc.
            items.add(VerifyHashesRequest.HashItem.builder()
                    .batchIdHex(pallet.getChainBatchIdHex())
                    .dataHashHex(BlockchainVerificationUtils.calculatePalletHash(pallet)) // Tính Hash Off-chain
                    .type("TRANSFORMED") // Phân loại là lô thành phẩm
                    .build());
            
            // 3.2. Tính toán lại mã băm cho toàn bộ các Nguyên liệu gốc tạo nên Pallet này
            for (RawBatch raw : rawBatches) {
                items.add(VerifyHashesRequest.HashItem.builder()
                        .batchIdHex(raw.getBatchIdHex())
                        .dataHashHex(BlockchainVerificationUtils.calculateRawBatchHash(raw)) // Tính Hash Off-chain
                        .type("RAW") // Phân loại là nguyên liệu thô
                        .build());
            }

            VerifyHashesRequest request = VerifyHashesRequest.builder().items(items).build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<VerifyHashesRequest> entity = new HttpEntity<>(request, headers);

            // 3.3. Gửi danh sách Hash vừa tính được sang Blockchain Service
            // API này sẽ lấy Hash gửi lên đi so sánh với Hash gốc lưu trên Smart Contract
            ResponseEntity<ApiResponse<VerifyHashesResponse>> resp = restTemplate.exchange(
                    blockchainBaseUrl + "/verify-hashes",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<VerifyHashesResponse>>() {}
            );

            // 3.4. Cập nhật kết quả xác thực cho từng sự kiện hiển thị trên App
            if (resp.getBody() != null && resp.getBody().getResult() != null) {
                VerifyHashesResponse result = resp.getBody().getResult();
                
                // Chuyển kết quả trả về thành dạng Map<BatchId, true/false> cho dễ tìm kiếm
                Map<String, Boolean> matchMap = result.getResults().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                VerifyHashesResponse.VerifyResult::getBatchIdHex,
                                VerifyHashesResponse.VerifyResult::isMatch
                        ));
                
                // Duyệt qua tất cả các sự kiện lịch sử (Tạo lô, Vận chuyển...)
                for (TraceHistoryEvent event : events) {
                    if (event.getTxHash() != null) {
                        if (matchMap.containsKey(event.getTxHash())) {
                            // Nếu là sự kiện Tạo Pallet/Nguyên liệu, lấy kết quả true/false từ Blockchain gán vào
                            event.setIsVerifiedOnChain(matchMap.get(event.getTxHash()));
                        } else if (event.getEventType().contains("TRANSFERRED")) {
                            // Lựa chọn 1: Các khâu vận chuyển coi như xác thực nội bộ (Internal Verified)
                            // Hiện tại các giao dịch chuyển giao (Transfer) đang tự động đánh dấu tick xanh
                            event.setIsVerifiedOnChain(true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Blockchain verification failed", e);
        }
    }

    private record ActorDetails(String name, String avatarUrl, String location) {}

    private ActorDetails fetchActorDetails(String actorId) {
        if (actorId == null || actorId.isBlank())
            return new ActorDetails(null, null, null);
        try {
            String url = identityServiceUrl + "/api/v1/users/directory/by-id/" + actorId;
            vn.edu.kma.common.dto.response.ApiResponse<?> response = restTemplate.getForObject(url,
                    vn.edu.kma.common.dto.response.ApiResponse.class);
            if (response != null && response.getResult() != null) {
                java.util.Map<?, ?> result = (java.util.Map<?, ?>) response.getResult();
                Object fullNameObj = result.get("fullName");
                Object avatarUrlObj = result.get("avatarUrl");
                Object locationObj = result.get("location");
                String name = fullNameObj != null && !fullNameObj.toString().isBlank() ? fullNameObj.toString() : null;
                String avatarUrl = avatarUrlObj != null && !avatarUrlObj.toString().isBlank() ? avatarUrlObj.toString() : null;
                String location = locationObj != null && !locationObj.toString().isBlank() ? locationObj.toString() : null;
                return new ActorDetails(name, avatarUrl, location);
            }
        } catch (Exception e) {
            log.warn("Could not fetch actor details for id: {}", actorId);
        }
        return new ActorDetails(null, null, null);
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
