package vn.edu.kma.traceability_core_service.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceNodeResponse {
    private String id;
    private String nodeType;
    private String code;
    private String name;
    private String actorId;
    private String actorName;
    private String actorAvatarUrl;
    private String location;
    private String occurredAt;
    private String quantity;
    private String unit;
    private String note;
    private String batchNo;
    private String expiryAt;
    private String packagingType;
    private String processingMethod;
    private String blockchainBatchIdHex;
    private boolean hasInputs;
    private String verificationStatus;
}
