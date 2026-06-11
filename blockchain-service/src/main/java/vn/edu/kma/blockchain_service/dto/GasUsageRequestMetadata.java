package vn.edu.kma.blockchain_service.dto;

import lombok.Builder;
import lombok.Value;
import vn.edu.kma.blockchain_service.domain.GasOperation;

@Value
@Builder
public class GasUsageRequestMetadata {
    String requestId;
    GasOperation operation;
    String entityId;
    String entityType;
    String sourceService;
    String billingActorId;
    String billingRole;
    String initiatedByUserId;
}
