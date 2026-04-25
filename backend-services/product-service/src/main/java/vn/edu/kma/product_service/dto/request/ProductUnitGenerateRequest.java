package vn.edu.kma.product_service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ProductUnitGenerateRequest {

    /**
     * Số unit cần sinh trong carton này (legacy). Nếu có {@code serials} thì ưu tiên theo serials.
     */
    private Integer count;

    /**
     * Danh sách serial tờ tiền dùng làm unitSerial. Nếu truyền thì mỗi serial tạo 1 unit.
     */
    private List<String> serials;
}
