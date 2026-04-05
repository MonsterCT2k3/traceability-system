package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnitClaimResponse {
    private String unitId;
    private String unitSerial;
    private String ownerId;
    private LocalDateTime claimedAt;
    private String ownerNameSnapshot;
}
