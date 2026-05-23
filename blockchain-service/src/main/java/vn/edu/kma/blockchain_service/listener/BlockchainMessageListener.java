package vn.edu.kma.blockchain_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import vn.edu.kma.blockchain_service.event.BlockchainOwnershipChangeEvent;
import vn.edu.kma.blockchain_service.event.BlockchainRecordBatchEvent;
import vn.edu.kma.blockchain_service.event.BlockchainRecordTransformedBatchEvent;
import vn.edu.kma.blockchain_service.event.BlockchainReplyEvent;
import vn.edu.kma.blockchain_service.service.TraceabilityService;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlockchainMessageListener {

    private final TraceabilityService traceabilityService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "blockchain.requests.batch", groupId = "blockchain-group")
    public void handleRecordBatch(String message) {
        log.info("Received raw message on batch topic: {}", message);
        try {
            BlockchainRecordBatchEvent event = objectMapper.readValue(message, BlockchainRecordBatchEvent.class);
            String txHash = traceabilityService.recordBatch(event.getBatchIdHex(), event.getDataHashHex());
            replySuccess(event.getEntityId(), event.getEntityType(), txHash);
        } catch (Exception e) {
            log.error("Error recording batch to blockchain", e);
            // Cố gắng lấy entityId nếu parse được một phần
            replyError("UNKNOWN", "RAW_BATCH", e.getMessage());
        }
    }

    @KafkaListener(topics = "blockchain.requests.transformed", groupId = "blockchain-group")
    public void handleRecordTransformedBatch(String message) {
        log.info("Received raw message on transformed topic: {}", message);
        try {
            BlockchainRecordTransformedBatchEvent event = objectMapper.readValue(message, BlockchainRecordTransformedBatchEvent.class);
            String txHash = traceabilityService.recordTransformedBatch(
                    event.getBatchIdHex(),
                    event.getDataHashHex(),
                    event.getParentHashesHex() != null ? event.getParentHashesHex() : java.util.Collections.emptyList()
            );
            replySuccess(event.getEntityId(), event.getEntityType(), txHash);
        } catch (Exception e) {
            log.error("Error recording transformed batch to blockchain", e);
            replyError("UNKNOWN", "PALLET", e.getMessage());
        }
    }

    @KafkaListener(topics = "blockchain.requests.ownership", groupId = "blockchain-group")
    public void handleOwnershipChange(String message) {
        log.info("Received raw message on ownership topic: {}", message);
        try {
            BlockchainOwnershipChangeEvent event = objectMapper.readValue(message, BlockchainOwnershipChangeEvent.class);
            String txHash = traceabilityService.logOwnershipChange(
                    event.getBatchIdHex(),
                    event.getFromUserId(),
                    event.getToUserId()
            );
            replySuccess(event.getEntityId(), event.getEntityType(), txHash);
        } catch (Exception e) {
            log.error("Error logging ownership change to blockchain", e);
            replyError("UNKNOWN", "TRANSFER", e.getMessage());
        }
    }

    private void replySuccess(String entityId, String entityType, String txHash) {
        BlockchainReplyEvent reply = BlockchainReplyEvent.builder()
                .entityId(entityId)
                .entityType(entityType)
                .txHash(txHash)
                .status("SUCCESS")
                .build();
        kafkaTemplate.send("blockchain.replies", reply);
    }

    private void replyError(String entityId, String entityType, String errorMessage) {
        BlockchainReplyEvent reply = BlockchainReplyEvent.builder()
                .entityId(entityId)
                .entityType(entityType)
                .status("ERROR")
                .errorMessage(errorMessage)
                .build();
        kafkaTemplate.send("blockchain.replies", reply);
    }
}
