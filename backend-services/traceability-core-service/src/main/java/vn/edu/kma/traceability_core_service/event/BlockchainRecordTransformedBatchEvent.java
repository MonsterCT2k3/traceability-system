package vn.edu.kma.traceability_core_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainRecordTransformedBatchEvent implements Serializable {
    private String requestId;
    private String operation;
    private String entityId;
    private String entityType; // "PALLET", "PRODUCT_UNIT", vv.
    private String batchIdHex;
    private String dataHashHex;
    private List<String> parentHashesHex;
    private String billingActorId;
    private String billingRole;
    private String initiatedByUserId;
    private String sourceService;
}

