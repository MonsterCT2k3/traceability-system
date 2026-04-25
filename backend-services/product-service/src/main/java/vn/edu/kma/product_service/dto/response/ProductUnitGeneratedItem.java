package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một dòng trong response generate unit. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnitGeneratedItem {
    private String unitId;
    private String unitSerial;
}
