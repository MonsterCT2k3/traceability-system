package vn.edu.kma.product_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.product_service.entity.Pallet;
import vn.edu.kma.product_service.entity.RawBatch;
import vn.edu.kma.product_service.entity.TradeOrder;
import vn.edu.kma.product_service.entity.TransferRecord;
import vn.edu.kma.product_service.repository.PalletRepository;
import vn.edu.kma.product_service.repository.RawBatchRepository;
import vn.edu.kma.product_service.repository.TradeOrderRepository;
import vn.edu.kma.product_service.repository.TransferRecordRepository;
import vn.edu.kma.product_service.event.BlockchainReplyEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlockchainReplyListener {

    private final RawBatchRepository rawBatchRepository;
    private final PalletRepository palletRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final TradeOrderRepository tradeOrderRepository;
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
                            palletRepository.delete(p);
                        }
                        notifyUser(p.getOwnerId(), "PALLET", entityId, success, txHash, errorMsg);
                    });
                    break;
                case "TRANSFER":
                    transferRecordRepository.findById(entityId).ifPresent(t -> {
                        if (success) {
                            t.setBlockchainStatus("OK");
                            t.setBlockchainTxHash(txHash);
                            transferRecordRepository.save(t);
                        } else {
                            log.error("TRANSFER {} blockchain error: {}. Xóa khỏi DB (Compensation)", entityId, errorMsg);
                            transferRecordRepository.delete(t);
                        }
                        notifyUser(t.getToUserId(), "TRANSFER", entityId, success, txHash, errorMsg);
                        notifyUser(t.getFromUserId(), "TRANSFER", entityId, success, txHash, errorMsg);
                    });
                    break;
                case "TRADE_ORDER":
                    tradeOrderRepository.findById(entityId).ifPresent(o -> {
                        if (success) {
                            o.setDeliveryChainStatus("OK");
                            o.setDeliveryTxHash(txHash);
                        } else {
                            o.setDeliveryChainStatus("FAILED");
                            o.setDeliveryChainError(errorMsg);
                            log.error("TRADE_ORDER {} blockchain error: {}", entityId, errorMsg);
                        }
                        tradeOrderRepository.save(o);
                        notifyUser(o.getBuyerId(), "TRADE_ORDER", entityId, success, txHash, errorMsg);
                        notifyUser(o.getSellerId(), "TRADE_ORDER", entityId, success, txHash, errorMsg);
                    });
                    break;
                default:
                    log.warn("Unknown entity type in blockchain reply: {}", entityType);
            }
        } catch (Exception e) {
            log.error("Error processing blockchain reply", e);
        }
    }

    private void notifyUser(String userId, String type, String entityId, boolean success, String txHash, String errorMsg) {
        if (userId == null) return;
        String topic = "/topic/blockchain-updates/" + userId;
        
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", type);
        payload.put("entityId", entityId);
        payload.put("success", success);
        if (success) {
            payload.put("txHash", txHash);
        } else {
            payload.put("error", errorMsg);
        }
        
        messagingTemplate.convertAndSend(topic, payload);
        log.info("Sent WS message to {}: {}", topic, payload);
    }
}
