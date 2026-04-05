package vn.edu.kma.product_service.dto.request;

import lombok.Data;

@Data
public class ProductUnitGenerateRequest {

    /**
     * Số unit cần sinh trong carton này.
     */
    private Integer count;
}
