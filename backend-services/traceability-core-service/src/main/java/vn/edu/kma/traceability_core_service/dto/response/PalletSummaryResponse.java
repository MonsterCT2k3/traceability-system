package vn.edu.kma.traceability_core_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PalletSummaryResponse {
    private String id;
    private String palletCode;
    private String palletName;
    private String batchNo;
    private String productId;
    private String inputStatus;
    private String ownerId;
    private String manufacturerId;
    private boolean hasInputs;
    private LocalDateTime createdAt;
}

