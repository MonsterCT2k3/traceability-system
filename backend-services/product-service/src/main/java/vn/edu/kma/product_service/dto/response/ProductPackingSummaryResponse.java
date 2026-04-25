package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPackingSummaryResponse {
    private String productId;
    private String productName;
    private long cartonsCount;
    private long unitsCount;
}
