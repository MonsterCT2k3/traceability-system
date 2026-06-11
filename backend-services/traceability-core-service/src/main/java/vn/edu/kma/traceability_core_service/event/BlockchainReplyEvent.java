package vn.edu.kma.traceability_core_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainReplyEvent implements Serializable {
    private String requestId;
    private String operation;
    private String entityId;
    private String entityType;
    private String txHash;
    private String status; // "SUCCESS" or "ERROR"
    private String gasStatus;
    private String billingActorId;
    private String billingRole;
    private String gasUsed;
    private String effectiveGasPriceWei;
    private String feeWei;
    private String blockNumber;
    private String errorCode;
    private String errorMessage;
}

