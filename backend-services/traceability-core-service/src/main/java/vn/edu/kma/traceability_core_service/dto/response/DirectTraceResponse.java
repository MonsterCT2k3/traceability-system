package vn.edu.kma.traceability_core_service.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectTraceResponse {
    private TraceNodeResponse currentNode;
    private List<TraceNodeResponse> directInputs;
    private String verificationScope;
    private DirectVerificationSummary verificationSummary;
}
