package vn.edu.kma.product_service.dto.request;

import lombok.Data;

@Data
public class PalletBulkPackingRequest {

    /** Số thùng (carton) tạo trong đợt này. */
    private Integer cartonCount;

    /** Số unit (hộp) mỗi thùng; phải khớp plannedUnitCount của từng carton. */
    private Integer unitsPerCarton;

    private String unitLabel;
    private String note;
}
