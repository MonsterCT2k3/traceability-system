package vn.edu.kma.traceability_core_service.dto.request;

import lombok.Data;

@Data
public class CartonCreateRequest {

    /**
     * Tuỳ chọn. Nếu để trống, server tự sinh: CTN-{palletCode}-{seq 4 số}, seq tăng theo pallet.
     */
    private String cartonCode;
    private Integer plannedUnitCount;
    private String unitLabel;
    private String note;
}

