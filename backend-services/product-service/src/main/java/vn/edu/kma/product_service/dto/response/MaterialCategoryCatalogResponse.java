package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCategoryCatalogResponse {
    private String id;
    private String code;
    private String label;
    private List<MaterialItemOptionResponse> items;
}
