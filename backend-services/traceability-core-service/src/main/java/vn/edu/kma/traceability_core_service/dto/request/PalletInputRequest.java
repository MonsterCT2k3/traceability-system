package vn.edu.kma.traceability_core_service.dto.request;

import lombok.Data;
import vn.edu.kma.traceability_core_service.domain.PalletInputType;

@Data
public class PalletInputRequest {
    private PalletInputType type;
    private String id;
}
