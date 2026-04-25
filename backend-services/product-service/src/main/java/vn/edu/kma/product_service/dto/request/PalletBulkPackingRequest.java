package vn.edu.kma.product_service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PalletBulkPackingRequest {

    /** Số thùng (carton) tạo trong đợt này. */
    private Integer cartonCount;

    /** Số unit (hộp) mỗi thùng; phải khớp plannedUnitCount của từng carton. */
    private Integer unitsPerCarton;

    /**
     * Luồng mới: mỗi phần tử là danh sách serial cho 1 carton.
     * Nếu có trường này thì ưu tiên dùng serialBatches, bỏ qua cartonCount/unitsPerCarton.
     */
    private List<List<String>> serialBatches;

    private String unitLabel;
    private String note;
}
