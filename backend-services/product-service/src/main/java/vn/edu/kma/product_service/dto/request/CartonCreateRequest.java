package vn.edu.kma.product_service.dto.request;

import lombok.Data;

@Data
public class CartonCreateRequest {

    private String cartonCode;
    private Integer plannedUnitCount;
    private String unitLabel;
    private String note;
}
