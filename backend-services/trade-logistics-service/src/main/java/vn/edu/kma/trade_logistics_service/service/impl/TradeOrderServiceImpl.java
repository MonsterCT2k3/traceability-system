package vn.edu.kma.trade_logistics_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.trade_logistics_service.service.BlockchainClient;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.common.security.UserRole;
import vn.edu.kma.common.event.TradeOrderEvent;
import vn.edu.kma.trade_logistics_service.domain.OrderType;
import vn.edu.kma.trade_logistics_service.domain.TradeOrderStatus;
import vn.edu.kma.trade_logistics_service.dto.request.AssignCarrierRequest;
import vn.edu.kma.trade_logistics_service.dto.request.TradeOrderCreateRequest;
import vn.edu.kma.trade_logistics_service.dto.request.TradeOrderLineRequest;
import vn.edu.kma.trade_logistics_service.dto.response.TradeOrderLineResponse;
import vn.edu.kma.trade_logistics_service.dto.response.TradeOrderResponse;
import vn.edu.kma.trade_logistics_service.client.CatalogClient;
import vn.edu.kma.trade_logistics_service.entity.TradeOrder;
import vn.edu.kma.trade_logistics_service.entity.TradeOrderLine;
import vn.edu.kma.trade_logistics_service.entity.TransferRecord;
import vn.edu.kma.trade_logistics_service.repository.TradeOrderRepository;
import vn.edu.kma.trade_logistics_service.repository.TransferRecordRepository;
import vn.edu.kma.trade_logistics_service.client.ProductClient;
import vn.edu.kma.trade_logistics_service.service.TradeOrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeOrderRepository tradeOrderRepository;
    private final ProductClient productClient;
    private final CatalogClient catalogClient;
    private final TransferRecordRepository transferRecordRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public TradeOrderResponse createOrder(TradeOrderCreateRequest request, String token) {
        try {
            String buyerId = extractUserIdFromToken(token);
            UserRole role = extractRoleFromToken(token);
            if (request.getOrderType() == null) {
                throw new RuntimeException("Thiếu orderType");
            }
            String sellerId = requireNonBlank(request.getSellerId(), "Thiếu sellerId");
            if (sellerId.equals(buyerId)) {
                throw new RuntimeException("Người mua và người bán không được trùng");
            }
            if (request.getLines() == null || request.getLines().isEmpty()) {
                throw new RuntimeException("Đơn phải có ít nhất một dòng");
            }

            if (request.getOrderType() == OrderType.MANUFACTURER_TO_SUPPLIER) {
                if (role != UserRole.MANUFACTURER) {
                    throw new RuntimeException("Chỉ nhà sản xuất được đặt đơn nguyên liệu từ NCC");
                }
                validateAndBuildLinesM2S(request.getLines(), sellerId);
            } else if (request.getOrderType() == OrderType.RETAILER_TO_MANUFACTURER) {
                if (role != UserRole.RETAILER) {
                    throw new RuntimeException("Chỉ retailer được đặt đơn tới NSX");
                }
                validateLinesRetail(request.getLines(), sellerId);
            } else {
                throw new RuntimeException("orderType không hợp lệ");
            }

            String orderCode = "ORD-" + System.currentTimeMillis();
            TradeOrder order = TradeOrder.builder()
                    .orderCode(orderCode)
                    .orderType(request.getOrderType())
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .status(TradeOrderStatus.PENDING)
                    .note(request.getNote())
                    .lines(new ArrayList<>())
                    .build();

            int idx = 0;
            for (TradeOrderLineRequest lr : request.getLines()) {
                TradeOrderLine line = TradeOrderLine.builder()
                        .order(order)
                        .lineIndex(idx++)
                        .targetRawBatchId(lr.getTargetRawBatchId())
                        .quantityRequested(trimOrNull(lr.getQuantityRequested()))
                        .unit(trimOrNull(lr.getUnit()))
                        .productId(lr.getProductId())
                        .quantityCartons(lr.getQuantityCartons())
                        .build();
                order.getLines().add(line);
            }

            TradeOrder saved = tradeOrderRepository.save(order);
            return toResponse(saved);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("createOrder", e);
            throw new RuntimeException("Lỗi tạo đơn: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TradeOrderResponse getById(String orderId, String token) {
        try {
            TradeOrder order = tradeOrderRepository.findByIdWithLines(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn không tồn tại"));
            assertCanView(order, token);
            return toResponse(order);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc đơn: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeOrderResponse> listAsBuyer(String token) {
        try {
            String uid = extractUserIdFromToken(token);
            return tradeOrderRepository.findByBuyerIdOrderByCreatedAtDesc(uid).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi danh sách đơn (người mua): " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeOrderResponse> listAsSeller(String token) {
        try {
            String uid = extractUserIdFromToken(token);
            return tradeOrderRepository.findBySellerIdOrderByCreatedAtDesc(uid).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi danh sách đơn (người bán): " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeOrderResponse> listAsCarrier(String token) {
        try {
            String uid = extractUserIdFromToken(token);
            return tradeOrderRepository.findByCarrierIdOrderByCreatedAtDesc(uid).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi danh sách đơn (vận chuyển): " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TradeOrderResponse accept(String orderId, String token) {
        try {
            String uid = extractUserIdFromToken(token);
            TradeOrder order = tradeOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn không tồn tại"));
            if (!order.getSellerId().equals(uid)) {
                throw new RuntimeException("Chỉ người bán được chấp nhận đơn");
            }
            if (order.getStatus() != TradeOrderStatus.PENDING) {
                throw new RuntimeException("Đơn không ở trạng thái chờ xử lý");
            }
            if (order.getOrderType() == OrderType.RETAILER_TO_MANUFACTURER) {
                for (TradeOrderLine line : order.getLines()) {
                    String productId = line.getProductId();
                    Integer qty = line.getQuantityCartons();
                    if (productId == null || qty == null || qty < 1) continue;
                    
                    ApiResponse<Boolean> checkRes = productClient.checkInventory(uid, productId, qty);
                    if (checkRes == null || Boolean.FALSE.equals(checkRes.getResult())) {
                        throw new RuntimeException("Không đủ hàng, vui lòng sản xuất và đóng gói thêm");
                    }
                }

                order.setStatus(TradeOrderStatus.PROCESSING);
                TradeOrder saved = tradeOrderRepository.save(order);
                reserveCartonsForRetailOrder(saved);
                return toResponse(saved);
            } else {
                order.setStatus(TradeOrderStatus.ACCEPTED);
                return toResponse(tradeOrderRepository.save(order));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi chấp nhận đơn: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TradeOrderResponse reject(String orderId, String token) {
        try {
            String uid = extractUserIdFromToken(token);
            TradeOrder order = tradeOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn không tồn tại"));
            if (!order.getSellerId().equals(uid)) {
                throw new RuntimeException("Chỉ người bán được từ chối đơn");
            }
            if (order.getStatus() != TradeOrderStatus.PENDING) {
                throw new RuntimeException("Đơn không ở trạng thái chờ xử lý");
            }
            order.setStatus(TradeOrderStatus.REJECTED);
            return toResponse(tradeOrderRepository.save(order));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi từ chối đơn: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TradeOrderResponse cancel(String orderId, String token) {
        try {
            String uid = extractUserIdFromToken(token);
            TradeOrder order = tradeOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn không tồn tại"));
            if (!order.getBuyerId().equals(uid)) {
                throw new RuntimeException("Chỉ người mua được hủy đơn");
            }
            if (order.getStatus() != TradeOrderStatus.PENDING) {
                throw new RuntimeException("Chỉ hủy được đơn đang chờ xử lý");
            }
            order.setStatus(TradeOrderStatus.CANCELLED);
            return toResponse(tradeOrderRepository.save(order));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hủy đơn: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TradeOrderResponse assignCarrier(String orderId, AssignCarrierRequest body, String token) {
        try {
            String uid = extractUserIdFromToken(token);
            String carrierId = requireNonBlank(body.getCarrierId(), "Thiếu carrierId");
            TradeOrder order = tradeOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn không tồn tại"));
            if (!order.getSellerId().equals(uid)) {
                throw new RuntimeException("Chỉ người bán được gán đơn vị vận chuyển");
            }
            if (order.getStatus() != TradeOrderStatus.ACCEPTED) {
                throw new RuntimeException("Chỉ gán VC khi đơn đã được chấp nhận");
            }
            order.setCarrierId(carrierId.trim());
            order.setStatus(TradeOrderStatus.ASSIGNED_TO_CARRIER);
            return toResponse(tradeOrderRepository.save(order));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi gán VC: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TradeOrderResponse confirmPickedUpFromSeller(String orderId, String token) {
        try {
            String actorId = extractUserIdFromToken(token);
            TradeOrder order = tradeOrderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn không tồn tại"));

            String carrierId = order.getCarrierId();
            if (carrierId == null || carrierId.isBlank()) {
                throw new RuntimeException("Đơn chưa có đơn vị vận chuyển");
            }
            if (!carrierId.equals(actorId)) {
                throw new RuntimeException("Chỉ đơn vị vận chuyển được xác nhận đã nhận hàng từ người bán");
            }
            if (order.getStatus() != TradeOrderStatus.ASSIGNED_TO_CARRIER) {
                throw new RuntimeException("Chỉ xác nhận nhận hàng khi đơn đang chờ VC đến lấy hàng tại người bán");
            }

            order.setStatus(TradeOrderStatus.PICKED_UP_FROM_SELLER);
            return toResponse(tradeOrderRepository.save(order));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("confirmPickedUpFromSeller", e);
            throw new RuntimeException("Lỗi xác nhận nhận hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TradeOrderResponse confirmDelivered(String orderId, String token) {
        try {
            String actorId = extractUserIdFromToken(token);
            TradeOrder order = tradeOrderRepository.findByIdWithLines(orderId)
                    .orElseThrow(() -> new RuntimeException("Đơn không tồn tại"));

            boolean hasCarrier = order.getCarrierId() != null && !order.getCarrierId().isBlank();
            if (hasCarrier) {
                if (!order.getCarrierId().equals(actorId)) {
                    throw new RuntimeException("Chỉ đơn vị vận chuyển được xác nhận đã giao (đơn đã gán VC)");
                }
                if (order.getStatus() != TradeOrderStatus.PICKED_UP_FROM_SELLER) {
                    throw new RuntimeException(
                            "VC cần xác nhận đã nhận hàng từ người bán trước, sau đó mới xác nhận giao cho người mua");
                }
            } else {
                if (!order.getSellerId().equals(actorId)) {
                    throw new RuntimeException("Chỉ người bán được xác nhận giao trực tiếp (chưa gán VC)");
                }
                if (order.getStatus() != TradeOrderStatus.ACCEPTED) {
                    throw new RuntimeException("Đơn chưa được chấp nhận");
                }
            }

            if (order.getOrderType() == OrderType.MANUFACTURER_TO_SUPPLIER) {
                finalizeM2SDelivery(order);
                order.setStatus(TradeOrderStatus.DELIVERED);
                TradeOrder saved = tradeOrderRepository.save(order);
                return toResponse(saved);
            } else if (order.getOrderType() == OrderType.RETAILER_TO_MANUFACTURER) {
                order.setStatus(TradeOrderStatus.PROCESSING);
                TradeOrder saved = tradeOrderRepository.save(order);
                requestRetailDelivery(saved);
                return toResponse(saved);
            } else {
                order.setDeliveryChainStatus("SKIPPED");
                order.setDeliveryChainError(null);
                order.setDeliveryTxHash(null);
                order.setStatus(TradeOrderStatus.DELIVERED);
                TradeOrder saved = tradeOrderRepository.save(order);
                return toResponse(saved);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("confirmDelivered", e);
            throw new RuntimeException("Lỗi xác nhận giao hàng: " + e.getMessage());
        }
    }

    private void finalizeM2SDelivery(TradeOrder order) {
        String sellerId = order.getSellerId();
        String buyerId = order.getBuyerId();
        String lastTx = null;
        StringBuilder err = new StringBuilder();

        for (TradeOrderLine line : order.getLines()) {
            String batchId = line.getTargetRawBatchId();
            if (batchId == null || batchId.isBlank()) {
                err.append("Dòng thiếu targetRawBatchId. ");
                continue;
            }
            Map<String, Object> batch = productClient.getRawBatch(batchId).getResult();
            if (batch == null) {
                throw new RuntimeException("Lô nguyên liệu không tồn tại: " + batchId);
            }
            if (!sellerId.equals(batch.get("ownerId"))) {
                throw new RuntimeException("Lô " + batchId + " không còn thuộc người bán (owner đã thay đổi)");
            }
            
            String batchIdHex = productClient.transferOwnership("RAW_BATCH", batchId, buyerId).getResult();
            String tx = callOwnershipChange(batchIdHex, sellerId, buyerId);
            if (tx != null && !tx.isBlank()) {
                lastTx = tx;
            } else {
                err.append("Lô ").append(batch.get("rawBatchCode")).append(": không nhận được tx. ");
            }
        }

        if (err.length() > 0) {
            order.setDeliveryChainStatus("PARTIAL_OR_FAILED");
            order.setDeliveryChainError(err.toString().trim());
        } else {
            order.setDeliveryChainStatus("OK");
            order.setDeliveryChainError(null);
        }
        order.setDeliveryTxHash(lastTx);
    }

    private void requestRetailDelivery(TradeOrder order) {
        String sellerId = order.getSellerId();
        List<TradeOrderEvent.OrderItem> items = new ArrayList<>();
        
        for (TradeOrderLine line : order.getLines()) {
            String productId = line.getProductId();
            Integer qty = line.getQuantityCartons();
            if (productId == null || qty == null || qty < 1) continue;

            items.add(TradeOrderEvent.OrderItem.builder()
                .productId(productId)
                .quantity(qty)
                .build());
        }

        if (!items.isEmpty()) {
            TradeOrderEvent event = TradeOrderEvent.builder()
                .orderId(order.getId())
                .sellerId(sellerId)
                .buyerId(order.getBuyerId())
                .items(items)
                .eventType("DELIVER")
                .build();
            kafkaTemplate.send("trade.order.events", event);
        }
    }

    private String callOwnershipChange(String batchIdHex, String fromUserId, String toUserId) {
        if (batchIdHex == null || batchIdHex.isBlank()) {
            return null;
        }
        try {
            // Note: Since this is called in a loop for M2S delivery, we can't wait for txHash immediately.
            // We'll return "PENDING" and the background listener will update it.
            vn.edu.kma.trade_logistics_service.event.BlockchainOwnershipChangeEvent event = 
                vn.edu.kma.trade_logistics_service.event.BlockchainOwnershipChangeEvent.builder()
                    .entityId("TRADE_" + System.currentTimeMillis()) // Using a placeholder or we can pass trade_order_id if we want
                    .entityType("TRADE_ORDER")
                    .batchIdHex(batchIdHex.trim())
                    .fromUserId(fromUserId)
                    .toUserId(toUserId)
                    .build();
            kafkaTemplate.send("blockchain.requests.ownership", event);
            return "PENDING";
        } catch (Exception e) {
            log.warn("Lỗi gọi ownership-change: {}", e.getMessage());
            return null;
        }
    }

    private void validateAndBuildLinesM2S(List<TradeOrderLineRequest> lines, String sellerId) {
        Set<String> seenBatchIds = new HashSet<>();
        int i = 0;
        for (TradeOrderLineRequest lr : lines) {
            String batchId = requireNonBlank(lr.getTargetRawBatchId(), "Dòng " + i + ": thiếu targetRawBatchId");
            String qtyReq = requireNonBlank(lr.getQuantityRequested(), "Dòng " + i + ": thiếu quantityRequested");
            String unit = requireNonBlank(lr.getUnit(), "Dòng " + i + ": thiếu unit");

            String batchKey = batchId.trim();
            if (!seenBatchIds.add(batchKey)) {
                throw new RuntimeException("Không được trùng lô nguyên liệu trên nhiều dòng: " + batchKey);
            }
            Map<String, Object> batch = productClient.getRawBatch(batchKey).getResult();
            if (batch == null) {
                throw new RuntimeException("Lô nguyên liệu không tồn tại: " + batchId);
            }
            if (!sellerId.equals(batch.get("ownerId"))) {
                throw new RuntimeException("Lô " + batch.get("rawBatchCode") + " không thuộc người bán đã chọn");
            }
            if (!unit.trim().equalsIgnoreCase(((String)batch.get("unit")).trim())) {
                throw new RuntimeException("Đơn vị dòng " + i + " không khớp lô (" + batch.get("unit") + ")");
            }
            BigDecimal available = parseQuantity(batch.get("quantity") != null ? batch.get("quantity").toString() : "0");
            BigDecimal req = parseQuantity(qtyReq);
            if (req.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Số lượng đặt phải > 0 (dòng " + i + ")");
            }
            if (req.compareTo(available) > 0) {
                throw new RuntimeException("Số lượng đặt vượt tồn lô " + batch.get("rawBatchCode"));
            }
            if (lr.getProductId() != null || lr.getQuantityCartons() != null) {
                throw new RuntimeException("Đơn M→NCC không dùng productId/quantityCartons");
            }
            i++;
        }
    }

    private void validateLinesRetail(List<TradeOrderLineRequest> lines, String sellerId) {
        int i = 0;
        for (TradeOrderLineRequest lr : lines) {
            String productId = requireNonBlank(lr.getProductId(), "Dòng " + i + ": thiếu productId");
            if (lr.getQuantityCartons() == null || lr.getQuantityCartons() < 1) {
                throw new RuntimeException("Dòng " + i + ": quantityCartons phải ≥ 1");
            }
            ApiResponse<Map<String, Object>> pRes = catalogClient.getProductById(productId.trim());
            if (pRes == null || pRes.getResult() == null) {
                throw new RuntimeException("Sản phẩm không tồn tại: " + productId);
            }
            Map<String, Object> p = pRes.getResult();
            if (!sellerId.equals(p.get("ownerId"))) {
                throw new RuntimeException("Sản phẩm không thuộc người bán đã chọn");
            }
            if (lr.getTargetRawBatchId() != null || lr.getQuantityRequested() != null || lr.getUnit() != null) {
                throw new RuntimeException("Đơn retailer không dùng raw batch trên dòng");
            }
            i++;
        }
    }

    private void assertCanView(TradeOrder order, String token) throws Exception {
        UserRole role = extractRoleFromToken(token);
        if (role == UserRole.ADMIN) {
            return;
        }
        String uid = extractUserIdFromToken(token);
        if (uid.equals(order.getBuyerId()) || uid.equals(order.getSellerId())) {
            return;
        }
        if (order.getCarrierId() != null && order.getCarrierId().equals(uid)) {
            return;
        }
        throw new RuntimeException("Không có quyền xem đơn này");
    }

    private TradeOrderResponse toResponse(TradeOrder o) {
        List<TradeOrderLineResponse> lines = o.getLines() == null ? List.of() : o.getLines().stream()
                .sorted(java.util.Comparator.comparingInt(TradeOrderLine::getLineIndex))
                .map(l -> TradeOrderLineResponse.builder()
                        .id(l.getId())
                        .lineIndex(l.getLineIndex())
                        .targetRawBatchId(l.getTargetRawBatchId())
                        .quantityRequested(l.getQuantityRequested())
                        .unit(l.getUnit())
                        .productId(l.getProductId())
                        .quantityCartons(l.getQuantityCartons())
                        .build())
                .collect(Collectors.toList());

        return TradeOrderResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .orderType(o.getOrderType())
                .buyerId(o.getBuyerId())
                .sellerId(o.getSellerId())
                .status(o.getStatus())
                .carrierId(o.getCarrierId())
                .note(o.getNote())
                .deliveryTxHash(o.getDeliveryTxHash())
                .deliveryChainStatus(o.getDeliveryChainStatus())
                .deliveryChainError(o.getDeliveryChainError())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .lines(lines)
                .build();
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private static String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }

    private static BigDecimal parseQuantity(String q) {
        if (q == null || q.isBlank()) {
            return BigDecimal.ZERO;
        }
        String s = q.trim().replace(',', '.');
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Số lượng không hợp lệ: " + q);
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

    private static UserRole extractRoleFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        String raw = signedJWT.getJWTClaimsSet().getStringClaim("role");
        return UserRole.fromClaimOrDefault(raw);
    }
    private void reserveCartonsForRetailOrder(TradeOrder order) {
        String sellerId = order.getSellerId();
        List<TradeOrderEvent.OrderItem> items = new ArrayList<>();
        
        for (TradeOrderLine line : order.getLines()) {
            String productId = line.getProductId();
            Integer qty = line.getQuantityCartons();
            if (productId == null || qty == null || qty < 1) continue;

            items.add(TradeOrderEvent.OrderItem.builder()
                .productId(productId)
                .quantity(qty)
                .build());
        }

        if (!items.isEmpty()) {
            TradeOrderEvent event = TradeOrderEvent.builder()
                .orderId(order.getId())
                .sellerId(sellerId)
                .buyerId(order.getBuyerId())
                .items(items)
                .eventType("ACCEPT")
                .build();
            kafkaTemplate.send("trade.order.events", event);
        }
    }
}
