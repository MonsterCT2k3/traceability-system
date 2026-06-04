package vn.edu.kma.traceability_core_service.dto.request;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyTransformedDirectRequest {
    private String batchIdHex;
    private String dataHashHex;
    private List<String> parentBatchIdHexes;
}
