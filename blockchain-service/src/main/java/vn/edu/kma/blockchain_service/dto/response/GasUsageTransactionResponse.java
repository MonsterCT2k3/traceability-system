package vn.edu.kma.blockchain_service.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class GasUsageTransactionResponse {
    String id;
    String requestId;
    String txHash;
    String operation;
    String entityId;
    String entityType;
    String sourceService;
    String billingActorId;
    String billingRole;
    String initiatedByUserId;
    String status;
    String gasUsed;
    String effectiveGasPriceWei;
    String feeWei;
    String blockNumber;
    String errorCode;
    String errorMessage;
    Instant submittedAt;
    Instant minedAt;
    Instant createdAt;
    Instant updatedAt;
}
