package vn.edu.kma.traceability_core_service.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectVerificationSummary {
    private String currentNodeStatus;
    private String inputRelationStatus;
    private Integer verifiedInputCount;
    private Integer totalInputCount;
    private String overallStatus;
}
