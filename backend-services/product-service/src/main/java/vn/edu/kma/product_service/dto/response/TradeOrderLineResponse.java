package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderLineResponse {
    private String id;
    private Integer lineIndex;
    private String targetRawBatchId;
    private String quantityRequested;
    private String unit;
    private String productId;
    private Integer quantityCartons;
}
