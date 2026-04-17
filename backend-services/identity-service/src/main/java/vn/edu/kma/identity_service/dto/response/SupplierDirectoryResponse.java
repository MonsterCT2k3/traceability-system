package vn.edu.kma.identity_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin tối thiểu để NSX tìm và chọn NCC khi đặt hàng (không lộ email/sdt).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDirectoryResponse {
    private String id;
    private String username;
    private String fullName;
    /** Mô tả rút gọn để hiển thị gợi ý tìm kiếm */
    private String descriptionPreview;
}
