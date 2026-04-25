package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanknoteSerialBulkSaveResponse {
    private int inserted;
    /** Đã tồn tại trong DB (bất kỳ ai đăng ký trước). */
    private int skippedDuplicate;
    /** Không đúng định dạng (4–32 ký tự chữ/số/gạch ngang). */
    private int skippedInvalid;
}
