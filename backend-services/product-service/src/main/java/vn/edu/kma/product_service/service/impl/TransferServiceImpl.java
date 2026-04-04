package vn.edu.kma.product_service.service.impl;

import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.product_service.dto.request.TransferInitRequest;
import vn.edu.kma.product_service.entity.Pallet;
import vn.edu.kma.product_service.entity.RawBatch;
import vn.edu.kma.product_service.entity.TransferRecord;
import vn.edu.kma.product_service.repository.PalletRepository;
import vn.edu.kma.product_service.repository.RawBatchRepository;
import vn.edu.kma.product_service.repository.TransferRecordRepository;
import vn.edu.kma.product_service.service.TransferService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    private final TransferRecordRepository transferRecordRepository;
    private final PalletRepository palletRepository;
    private final RawBatchRepository rawBatchRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.url.blockchain-service}")
    private String blockchainBaseUrl;

    @Override
    public TransferRecord initiateTransfer(TransferInitRequest request, String token) {
        try {
            String currentUserId = extractUserIdFromToken(token);
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
                    .toUserId(newOwnerId);

            if ("PALLET".equals(targetType)) {
                Pallet pallet = palletRepository.findById(targetId)
                        .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));
                if (!currentUserId.equals(pallet.getOwnerId())) {
                    throw new RuntimeException("Bạn không phải owner hiện tại của pallet");
                }
                builder.palletId(pallet.getId());
                builder.productId(pallet.getProductId());
            } else {
                RawBatch rawBatch = rawBatchRepository.findById(targetId)
                        .orElseThrow(() -> new RuntimeException("RawBatch không tồn tại"));
                if (!currentUserId.equals(rawBatch.getOwnerId())) {
                    throw new RuntimeException("Bạn không phải owner hiện tại của raw batch");
                }
                builder.rawBatchId(rawBatch.getId());
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
                String batchIdHex;
                if ("PALLET".equals(transfer.getTargetType())) {
                    Pallet pallet = palletRepository.findById(transfer.getTargetId())
                            .orElseThrow(() -> new RuntimeException("Pallet không tồn tại"));
                    pallet.setOwnerId(transfer.getToUserId());
                    palletRepository.save(pallet);
                    batchIdHex = pallet.getChainBatchIdHex();
                } else if ("RAW_BATCH".equals(transfer.getTargetType())) {
                    RawBatch rawBatch = rawBatchRepository.findById(transfer.getTargetId())
                            .orElseThrow(() -> new RuntimeException("RawBatch không tồn tại"));
                    rawBatch.setOwnerId(transfer.getToUserId());
                    rawBatchRepository.save(rawBatch);
                    batchIdHex = rawBatch.getBatchIdHex();
                } else {
                    throw new RuntimeException("targetType không hợp lệ: " + transfer.getTargetType());
                }

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
            Map<String, String> body = new HashMap<>();
            body.put("batchIdHex", batchIdHex.trim());
            body.put("fromUserId", transfer.getFromUserId());
            body.put("toUserId", transfer.getToUserId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<ApiResponse<String>> resp = restTemplate.exchange(
                    blockchainBaseUrl + "/ownership-change",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<String>>() {}
            );
            ApiResponse<String> api = resp.getBody();
            if (api != null && api.getCode() == 200 && api.getResult() != null && !api.getResult().isBlank()) {
                transfer.setBlockchainTxHash(api.getResult());
                transfer.setBlockchainStatus("OK");
                transfer.setBlockchainError(null);
            } else {
                transfer.setBlockchainStatus("FAILED");
                transfer.setBlockchainError(api != null ? api.getMessage() : "Phản hồi blockchain rỗng");
                log.warn("ownership-change không trả txHash: {}", api);
            }
        } catch (Exception e) {
            transfer.setBlockchainStatus("FAILED");
            transfer.setBlockchainError(e.getMessage());
            log.warn("Lỗi gọi ownership-change: {}", e.getMessage());
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
        if (!"PALLET".equals(t) && !"RAW_BATCH".equals(t)) {
            throw new RuntimeException("targetType phải là PALLET hoặc RAW_BATCH");
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
}
