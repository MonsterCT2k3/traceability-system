package vn.edu.kma.product_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderLineRequest {

    /** MANUFACTURER_TO_SUPPLIER */
    private String targetRawBatchId;
    private String quantityRequested;
    private String unit;

    /** RETAILER_TO_MANUFACTURER */
    private String productId;
    private Integer quantityCartons;
}
