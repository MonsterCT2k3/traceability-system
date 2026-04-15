package vn.edu.kma.product_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawBatchMergeRequest {

    /** Tối thiểu 2 lô; cùng materialType + materialName; cùng owner. Sau khi tạo lô mới, các lô này bị xóa khỏi DB. */
    private List<String> sourceRawBatchIds;

    /** Ghi chú thêm cho lô sau gộp (tùy chọn). */
    private String note;

    /** Ghi đè vị trí kho cho lô mới; nếu null thì lấy từ một lô nguồn. */
    private String location;
}
