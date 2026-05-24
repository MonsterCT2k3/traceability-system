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
public class ProductUnitGenerateResponse {
    private String cartonId;
    private String cartonCode;
    private List<ProductUnitGeneratedItem> units;
}

