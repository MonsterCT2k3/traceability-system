package vn.edu.kma.traceability_core_service.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyTransformedDirectResponse {
    private boolean dataHashMatch;
    private boolean parentRootMatch;
    private String onChainParentRootHex;
    private String calculatedParentRootHex;
}
