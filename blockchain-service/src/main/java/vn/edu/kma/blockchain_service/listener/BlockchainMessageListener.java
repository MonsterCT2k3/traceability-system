package vn.edu.kma.blockchain_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import vn.edu.kma.blockchain_service.domain.GasOperation;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;
import vn.edu.kma.blockchain_service.dto.BlockchainExecutionResult;
import vn.edu.kma.blockchain_service.dto.GasUsageRequestMetadata;
import vn.edu.kma.blockchain_service.entity.BlockchainGasUsage;
import vn.edu.kma.blockchain_service.event.BlockchainOwnershipChangeEvent;
import vn.edu.kma.blockchain_service.event.BlockchainRecordBatchEvent;
import vn.edu.kma.blockchain_service.event.BlockchainRecordTransformedBatchEvent;
import vn.edu.kma.blockchain_service.event.BlockchainReplyEvent;
import vn.edu.kma.blockchain_service.service.GasUsageService;
import vn.edu.kma.blockchain_service.service.TraceabilityService;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlockchainMessageListener {

    private final TraceabilityService traceabilityService;
    private final GasUsageService gasUsageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "blockchain.requests.batch", groupId = "blockchain-group")
    public void handleRecordBatch(String message) {
        log.info("Received raw message on batch topic: {}", message);
        BlockchainRecordBatchEvent event = null;
        try {
            event = objectMapper.readValue(message, BlockchainRecordBatchEvent.class);
            GasUsageRequestMetadata metadata = metadata(event, GasOperation.RECORD_BATCH, "traceability-core-service");
            BlockchainGasUsage usage = gasUsageService.start(metadata);
            if (gasUsageService.isTerminal(usage)) {
                replyFromUsage(usage, null);
                return;
            }
            BlockchainExecutionResult result = traceabilityService.recordBatch(event.getBatchIdHex(), event.getDataHashHex());
            usage = gasUsageService.complete(metadata.getRequestId(), result);
            replyFromUsage(usage, result.getErrorMessage());
        } catch (Exception e) {
            log.error("Error recording batch to blockchain", e);
            completeAndReplyError(event, GasOperation.RECORD_BATCH, "traceability-core-service", "RAW_BATCH", e);
        }
    }

    @KafkaListener(topics = "blockchain.requests.transformed", groupId = "blockchain-group")
    public void handleRecordTransformedBatch(String message) {
        log.info("Received raw message on transformed topic: {}", message);
        BlockchainRecordTransformedBatchEvent event = null;
        try {
            event = objectMapper.readValue(message, BlockchainRecordTransformedBatchEvent.class);
            GasUsageRequestMetadata metadata = metadata(event, GasOperation.RECORD_TRANSFORMED_BATCH, "traceability-core-service");
            BlockchainGasUsage usage = gasUsageService.start(metadata);
            if (gasUsageService.isTerminal(usage)) {
                replyFromUsage(usage, null);
                return;
            }
            BlockchainExecutionResult result = traceabilityService.recordTransformedBatch(
                    event.getBatchIdHex(),
                    event.getDataHashHex(),
                    event.getParentHashesHex()
            );
            usage = gasUsageService.complete(metadata.getRequestId(), result);
            replyFromUsage(usage, result.getErrorMessage());
        } catch (Exception e) {
            log.error("Error recording transformed batch to blockchain", e);
            completeAndReplyError(event, GasOperation.RECORD_TRANSFORMED_BATCH, "traceability-core-service", "PALLET", e);
        }
    }

    @KafkaListener(topics = "blockchain.requests.ownership", groupId = "blockchain-group")
    public void handleOwnershipChange(String message) {
        log.info("Received raw message on ownership topic: {}", message);
        BlockchainOwnershipChangeEvent event = null;
        try {
            event = objectMapper.readValue(message, BlockchainOwnershipChangeEvent.class);
            GasUsageRequestMetadata metadata = metadata(event, GasOperation.OWNERSHIP_CHANGE, "trade-logistics-service");
            BlockchainGasUsage usage = gasUsageService.start(metadata);
            if (gasUsageService.isTerminal(usage)) {
                replyFromUsage(usage, null);
                return;
            }
            BlockchainExecutionResult result = traceabilityService.logOwnershipChange(
                    event.getBatchIdHex(),
                    event.getFromUserId(),
                    event.getToUserId()
            );
            usage = gasUsageService.complete(metadata.getRequestId(), result);
            replyFromUsage(usage, result.getErrorMessage());
        } catch (Exception e) {
            log.error("Error logging ownership change to blockchain", e);
            completeAndReplyError(event, GasOperation.OWNERSHIP_CHANGE, "trade-logistics-service", "TRANSFER", e);
        }
    }

    private String entityIdOrUnknown(BlockchainRecordBatchEvent event) {
        return event != null && event.getEntityId() != null ? event.getEntityId() : "UNKNOWN";
    }

    private String entityIdOrUnknown(BlockchainRecordTransformedBatchEvent event) {
        return event != null && event.getEntityId() != null ? event.getEntityId() : "UNKNOWN";
    }

    private String entityIdOrUnknown(BlockchainOwnershipChangeEvent event) {
        return event != null && event.getEntityId() != null ? event.getEntityId() : "UNKNOWN";
    }

    private String entityTypeOrDefault(BlockchainRecordBatchEvent event, String defaultValue) {
        return event != null && event.getEntityType() != null ? event.getEntityType() : defaultValue;
    }

    private String entityTypeOrDefault(BlockchainRecordTransformedBatchEvent event, String defaultValue) {
        return event != null && event.getEntityType() != null ? event.getEntityType() : defaultValue;
    }

    private String entityTypeOrDefault(BlockchainOwnershipChangeEvent event, String defaultValue) {
        return event != null && event.getEntityType() != null ? event.getEntityType() : defaultValue;
    }

    private GasUsageRequestMetadata metadata(BlockchainRecordBatchEvent event, GasOperation operation, String defaultSource) {
        return GasUsageRequestMetadata.builder()
                .requestId(firstNonBlank(event.getRequestId(), operation + ":" + event.getEntityType() + ":" + event.getEntityId()))
                .operation(operation)
                .entityId(event.getEntityId())
                .entityType(entityTypeOrDefault(event, "RAW_BATCH"))
                .sourceService(firstNonBlank(event.getSourceService(), defaultSource))
                .billingActorId(event.getBillingActorId())
                .billingRole(event.getBillingRole())
                .initiatedByUserId(event.getInitiatedByUserId())
                .build();
    }

    private GasUsageRequestMetadata metadata(BlockchainRecordTransformedBatchEvent event, GasOperation operation, String defaultSource) {
        return GasUsageRequestMetadata.builder()
                .requestId(firstNonBlank(event.getRequestId(), operation + ":" + event.getEntityType() + ":" + event.getEntityId()))
                .operation(operation)
                .entityId(event.getEntityId())
                .entityType(entityTypeOrDefault(event, "PALLET"))
                .sourceService(firstNonBlank(event.getSourceService(), defaultSource))
                .billingActorId(event.getBillingActorId())
                .billingRole(event.getBillingRole())
                .initiatedByUserId(event.getInitiatedByUserId())
                .build();
    }

    private GasUsageRequestMetadata metadata(BlockchainOwnershipChangeEvent event, GasOperation operation, String defaultSource) {
        return GasUsageRequestMetadata.builder()
                .requestId(firstNonBlank(event.getRequestId(), operation + ":" + event.getEntityType() + ":" + event.getEntityId()))
                .operation(operation)
                .entityId(event.getEntityId())
                .entityType(entityTypeOrDefault(event, "TRANSFER"))
                .sourceService(firstNonBlank(event.getSourceService(), defaultSource))
                .billingActorId(event.getBillingActorId())
                .billingRole(event.getBillingRole())
                .initiatedByUserId(event.getInitiatedByUserId())
                .build();
    }

    private void completeAndReplyError(BlockchainRecordBatchEvent event, GasOperation operation, String sourceService, String defaultEntityType, Exception e) {
        if (event != null && event.getRequestId() != null && !event.getRequestId().isBlank()) {
            completeSubmissionFailure(event.getRequestId(), e);
        }
        replyError(event == null ? null : event.getRequestId(), operation.name(), entityIdOrUnknown(event),
                entityTypeOrDefault(event, defaultEntityType), event == null ? null : event.getBillingActorId(),
                event == null ? null : event.getBillingRole(), e.getClass().getSimpleName(), e.getMessage());
    }

    private void completeAndReplyError(BlockchainRecordTransformedBatchEvent event, GasOperation operation, String sourceService, String defaultEntityType, Exception e) {
        if (event != null && event.getRequestId() != null && !event.getRequestId().isBlank()) {
            completeSubmissionFailure(event.getRequestId(), e);
        }
        replyError(event == null ? null : event.getRequestId(), operation.name(), entityIdOrUnknown(event),
                entityTypeOrDefault(event, defaultEntityType), event == null ? null : event.getBillingActorId(),
                event == null ? null : event.getBillingRole(), e.getClass().getSimpleName(), e.getMessage());
    }

    private void completeAndReplyError(BlockchainOwnershipChangeEvent event, GasOperation operation, String sourceService, String defaultEntityType, Exception e) {
        if (event != null && event.getRequestId() != null && !event.getRequestId().isBlank()) {
            completeSubmissionFailure(event.getRequestId(), e);
        }
        replyError(event == null ? null : event.getRequestId(), operation.name(), entityIdOrUnknown(event),
                entityTypeOrDefault(event, defaultEntityType), event == null ? null : event.getBillingActorId(),
                event == null ? null : event.getBillingRole(), e.getClass().getSimpleName(), e.getMessage());
    }

    private void completeSubmissionFailure(String requestId, Exception e) {
        try {
            gasUsageService.findByRequestId(requestId).ifPresent(usage -> {
                if (!gasUsageService.isTerminal(usage)) {
                    gasUsageService.complete(requestId, BlockchainExecutionResult.builder()
                            .status(GasUsageStatus.SUBMISSION_FAILED)
                            .errorCode(e.getClass().getSimpleName())
                            .errorMessage(e.getMessage())
                            .build());
                }
            });
        } catch (Exception completeError) {
            log.warn("Cannot complete failed gas usage request {}: {}", requestId, completeError.getMessage());
        }
    }

    private void replyFromUsage(BlockchainGasUsage usage, String fallbackErrorMessage) {
        boolean success = usage.getStatus() == GasUsageStatus.SUCCESS;
        BlockchainReplyEvent reply = BlockchainReplyEvent.builder()
                .requestId(usage.getRequestId())
                .operation(usage.getOperation().name())
                .entityId(usage.getEntityId())
                .entityType(usage.getEntityType())
                .txHash(usage.getTxHash())
                .status(success ? "SUCCESS" : "ERROR")
                .gasStatus(usage.getStatus().name())
                .billingActorId(usage.getBillingActorId())
                .billingRole(usage.getBillingRole())
                .gasUsed(toStringOrNull(usage.getGasUsed()))
                .effectiveGasPriceWei(toStringOrNull(usage.getEffectiveGasPriceWei()))
                .feeWei(toStringOrNull(usage.getFeeWei()))
                .blockNumber(toStringOrNull(usage.getBlockNumber()))
                .errorCode(usage.getErrorCode())
                .errorMessage(firstNonBlank(usage.getErrorMessage(), fallbackErrorMessage))
                .build();
        kafkaTemplate.send("blockchain.replies", reply);
    }

    private void replySuccess(String requestId, String operation, String entityId, String entityType, String txHash) {
        BlockchainReplyEvent reply = BlockchainReplyEvent.builder()
                .requestId(requestId)
                .operation(operation)
                .entityId(entityId)
                .entityType(entityType)
                .txHash(txHash)
                .status("SUCCESS")
                .build();
        kafkaTemplate.send("blockchain.replies", reply);
    }

    private void replyError(String requestId, String operation, String entityId, String entityType,
                            String billingActorId, String billingRole, String errorCode, String errorMessage) {
        BlockchainReplyEvent reply = BlockchainReplyEvent.builder()
                .requestId(requestId)
                .operation(operation)
                .entityId(entityId)
                .entityType(entityType)
                .status("ERROR")
                .gasStatus(GasUsageStatus.SUBMISSION_FAILED.name())
                .billingActorId(billingActorId)
                .billingRole(billingRole)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
        kafkaTemplate.send("blockchain.replies", reply);
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String toStringOrNull(Object value) {
        return value == null ? null : value.toString();
    }
}
