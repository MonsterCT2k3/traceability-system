package vn.edu.kma.traceability_core_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PalletBulkCartonResult {
    private String cartonCode;
    private String ownerId;
    private String ownerName;
    private String status; // IN_STOCK, DELIVERED
    private List<PackingUnitSerialItem> units;
}

