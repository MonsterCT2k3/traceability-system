package vn.edu.kma.trade_logistics_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import vn.edu.kma.trade_logistics_service.service.BlockchainClient;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.trade_logistics_service.dto.request.TransferInitRequest;
import vn.edu.kma.trade_logistics_service.entity.TransferRecord;
import vn.edu.kma.trade_logistics_service.repository.TransferRecordRepository;
import vn.edu.kma.trade_logistics_service.client.ProductClient;
import vn.edu.kma.trade_logistics_service.service.TransferService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRecordRepository transferRecordRepository;
    private final ProductClient productClient;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public TransferRecord initiateTransfer(TransferInitRequest request, String token) {
        try {
            String currentUserId = extractUserIdFromToken(token);
            String currentUserRole = extractRoleFromToken(token);
            String targetType = normalizeTargetType(request.getTargetType());
            String targetId = requireNonBlank(request.getTargetId(), "Thiếu targetId");
            String newOwnerId = requireNonBlank(request.getNewOwnerId(), "Thiếu newOwnerId");

            if (currentUserId.equals(newOwnerId)) {
                throw new RuntimeException("Không thể chuyển cho chính mình!");
            }

            transferRecordRepository.findByTargetTypeAndTargetIdAndStatus(targetType, targetId, "PENDING")
                    .ifPresent(t -> {
                        throw new RuntimeException("Đối tượng này đang có yêu cầu chuyển giao chưa xử lý!");
                    });

            TransferRecord.TransferRecordBuilder builder = TransferRecord.builder()
                    .targetType(targetType)
                    .targetId(targetId)
                    .fromUserId(currentUserId)
                    .fromUserRole(currentUserRole)
                    .toUserId(newOwnerId);

            if ("PALLET".equals(targetType)) {
                Map<String, Object> pallet = productClient.getPallet(targetId).getResult();
                if (pallet == null) throw new RuntimeException("Pallet không tồn tại");
                if (!currentUserId.equals(pallet.get("ownerId"))) throw new RuntimeException("Bạn không phải owner hiện tại của pallet");
                builder.palletId((String) pallet.get("id"));
                builder.productId((String) pallet.get("productId"));
            } else if ("RAW_BATCH".equals(targetType)) {
                Map<String, Object> rawBatch = productClient.getRawBatch(targetId).getResult();
                if (rawBatch == null) throw new RuntimeException("RawBatch không tồn tại");
                if (!currentUserId.equals(rawBatch.get("ownerId"))) throw new RuntimeException("Bạn không phải owner hiện tại của raw batch");
                builder.rawBatchId((String) rawBatch.get("id"));
            } else if ("CARTON".equals(targetType)) {
                Map<String, Object> carton = productClient.getCarton(targetId).getResult();
                if (carton == null) throw new RuntimeException("Carton không tồn tại");
                if (!currentUserId.equals(carton.get("ownerId"))) throw new RuntimeException("Bạn không phải owner hiện tại của thùng hàng");
            } else if ("UNIT".equals(targetType)) {
                Map<String, Object> unit = productClient.getProductUnit(targetId).getResult();
                if (unit == null) throw new RuntimeException("Sản phẩm không tồn tại");
                if (!currentUserId.equals(unit.get("ownerId"))) throw new RuntimeException("Bạn không phải owner hiện tại của sản phẩm");
            }

            return transferRecordRepository.save(builder.build());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo yêu cầu chuyển giao: " + e.getMessage());
        }
    }

    @Override
    public TransferRecord respondTransfer(String transferId, boolean accept, String token) {
        try {
            String currentUserId = extractUserIdFromToken(token);
            TransferRecord transfer = transferRecordRepository.findById(transferId)
                    .orElseThrow(() -> new RuntimeException("Yêu cầu chuyển giao không tồn tại"));

            if (!transfer.getToUserId().equals(currentUserId)) {
                throw new RuntimeException("Bạn không phải người nhận!");
            }
            if (!"PENDING".equals(transfer.getStatus())) {
                throw new RuntimeException("Yêu cầu này đã được xử lý rồi!");
            }

            if (accept) {
                transfer.setStatus("ACCEPTED");
                String batchIdHex = productClient.transferOwnership(transfer.getTargetType(), transfer.getTargetId(), transfer.getToUserId()).getResult();


                callOwnershipChange(transfer, batchIdHex);
            } else {
                transfer.setStatus("REJECTED");
            }

            transfer.setUpdatedAt(LocalDateTime.now());
            return transferRecordRepository.save(transfer);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xử lý chuyển giao: " + e.getMessage());
        }
    }

    @Override
    public List<TransferRecord> getPendingTransfers(String token) {
        try {
            String currentUserId = extractUserIdFromToken(token);
            return transferRecordRepository.findByToUserIdAndStatus(currentUserId, "PENDING");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lấy pending transfer: " + e.getMessage());
        }
    }

    private void callOwnershipChange(TransferRecord transfer, String batchIdHex) {
        if (batchIdHex == null || batchIdHex.isBlank()) {
            transfer.setBlockchainStatus("SKIPPED");
            transfer.setBlockchainError("Không tìm thấy batchIdHex để audit ownership-change");
            return;
        }
        try {
            vn.edu.kma.trade_logistics_service.event.BlockchainOwnershipChangeEvent event = 
                vn.edu.kma.trade_logistics_service.event.BlockchainOwnershipChangeEvent.builder()
                    .entityId(transfer.getId())
                    .entityType("TRANSFER")
                    .batchIdHex(batchIdHex.trim())
                    .fromUserId(transfer.getFromUserId())
                    .toUserId(transfer.getToUserId())
                    .requestId("transfer:" + transfer.getId() + ":ownership")
                    .operation("OWNERSHIP_CHANGE")
                    .billingActorId(transfer.getFromUserId())
                    .billingRole(resolveBillingRole(transfer))
                    .initiatedByUserId(transfer.getToUserId())
                    .sourceService("trade-logistics-service")
                    .build();
            kafkaTemplate.send("blockchain.requests.ownership", event);

            transfer.setBlockchainStatus("PENDING");
            transfer.setBlockchainError(null);
        } catch (Exception e) {
            transfer.setBlockchainStatus("FAILED");
            transfer.setBlockchainError("Lỗi gửi Kafka event: " + e.getMessage());
            log.warn("Lỗi gửi ownership-change event: {}", e.getMessage());
        }
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private static String normalizeTargetType(String raw) {
        String t = requireNonBlank(raw, "Thiếu targetType").toUpperCase();
        if (!"PALLET".equals(t) && !"RAW_BATCH".equals(t) && !"CARTON".equals(t) && !"UNIT".equals(t)) {
            throw new RuntimeException("targetType phải là PALLET, RAW_BATCH, CARTON hoặc UNIT");
        }
        return t;
    }

    private static String extractUserIdFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("userId");
    }

    private static String resolveBillingRole(TransferRecord transfer) {
        if (transfer.getFromUserRole() != null && !transfer.getFromUserRole().isBlank()) {
            return transfer.getFromUserRole();
        }
        return "RAW_BATCH".equals(transfer.getTargetType()) ? "SUPPLIER" : "MANUFACTURER";
    }

    private static String extractRoleFromToken(String tokenHeader) throws Exception {
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.getJWTClaimsSet().getStringClaim("role");
    }
}
