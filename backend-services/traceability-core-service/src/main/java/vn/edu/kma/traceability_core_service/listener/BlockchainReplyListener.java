package vn.edu.kma.traceability_core_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.kma.traceability_core_service.domain.PalletInputStatus;
import vn.edu.kma.traceability_core_service.domain.PalletInputType;
import vn.edu.kma.traceability_core_service.entity.Pallet;
import vn.edu.kma.traceability_core_service.entity.PalletInput;
import vn.edu.kma.traceability_core_service.entity.RawBatch;
import vn.edu.kma.traceability_core_service.repository.PalletInputRepository;
import vn.edu.kma.traceability_core_service.repository.PalletRepository;
import vn.edu.kma.traceability_core_service.repository.RawBatchRepository;
import vn.edu.kma.traceability_core_service.event.BlockchainReplyEvent;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlockchainReplyListener {

    private final RawBatchRepository rawBatchRepository;
    private final PalletRepository palletRepository;
    private final PalletInputRepository palletInputRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "blockchain.replies", groupId = "product-group")
    @Transactional
    public void handleBlockchainReply(String message) {
        log.info("Received raw blockchain reply: {}", message);
        
        try {
            BlockchainReplyEvent event = objectMapper.readValue(message, BlockchainReplyEvent.class);
            log.info("Parsed blockchain reply: {}", event);

            String entityId = event.getEntityId();
            String entityType = event.getEntityType();
            boolean success = "SUCCESS".equals(event.getStatus());
            String txHash = event.getTxHash();
            String errorMsg = event.getErrorMessage();
            switch (entityType) {
                case "RAW_BATCH":
                    rawBatchRepository.findById(entityId).ifPresent(r -> {
                        if (success) {
                            r.setAnchorTxHash(txHash);
                            rawBatchRepository.save(r);
                        } else {
                            log.error("RAW_BATCH {} blockchain error: {}. Xóa khỏi DB (Compensation)", entityId, errorMsg);
                            rawBatchRepository.delete(r);
                        }
                        notifyUser(r.getOwnerId(), "RAW_BATCH", entityId, success, txHash, errorMsg);
                    });
                    break;
                case "PALLET":
                    palletRepository.findById(entityId).ifPresent(p -> {
                        if (success) {
                            p.setAnchorTxHash(txHash);
                            palletRepository.save(p);
                        } else {
                            log.error("PALLET {} blockchain error: {}. Xóa khỏi DB (Compensation)", entityId, errorMsg);
                            restoreConsumedInputPallets(entityId);
                            palletRepository.delete(p);
                        }
                        notifyUser(p.getOwnerId(), "PALLET", entityId, success, txHash, errorMsg);
                    });
                    break;

                default:
                    log.warn("Unknown entity type in blockchain reply: {}", entityType);
            }
        } catch (Exception e) {
            log.error("Error processing blockchain reply", e);
        }
    }

    private void restoreConsumedInputPallets(String outputPalletId) {
        List<PalletInput> inputs = palletInputRepository.findByOutputPalletIdOrderByCreatedAtAsc(outputPalletId);
        for (PalletInput input : inputs) {
            if (input.getInputType() != PalletInputType.PALLET) continue;

            palletRepository.findById(input.getInputId()).ifPresent(inputPallet -> {
                if (inputPallet.getInputStatus() == PalletInputStatus.CONSUMED) {
                    inputPallet.setInputStatus(PalletInputStatus.AVAILABLE);
                    palletRepository.save(inputPallet);
                }
            });
        }
        palletInputRepository.deleteAll(inputs);
        palletInputRepository.flush();
    }

    private void notifyUser(String userId, String type, String entityId, boolean success, String txHash, String errorMsg) {
        if (userId == null) return;

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", type);
        payload.put("entityId", entityId);
        payload.put("success", success);
        if (success) {
            payload.put("txHash", txHash);
        } else {
            payload.put("error", errorMsg);
        }

        sendAfterCommit(() -> {
            messagingTemplate.convertAndSendToUser(userId, "/queue/blockchain-updates", payload);
            log.info("Sent WS message to user {}: {}", userId, payload);
        });
    }

    private void sendAfterCommit(Runnable sendAction) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendAction.run();
                }
            });
        } else {
            sendAction.run();
        }
    }
}

