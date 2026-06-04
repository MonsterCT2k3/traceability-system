package vn.edu.kma.traceability_core_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackingUnitSerialItem {
    private String unitId;
    private String unitSerial;
    private String traceQrPayload;
    private String claimToken;
}

