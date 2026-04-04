package vn.edu.kma.product_service.dto.request;

import lombok.Data;

@Data
public class RawBatchCreateRequest {
    private String materialType; // MILK, RADISH, ...
    private String materialName; // "Sữa bò tươi nguyên liệu"

    /**
     * Ngày thu hoạch dạng string để bạn chuẩn hóa khi hash.
     * Ví dụ: 2026-03-23
     */
    private String harvestedAt;

    /**
     * Số lượng (dùng String để tránh lệch format kiểu 10000 vs 10000.0).
     */
    private String quantity;

    private String unit; // kg, litre, box...
    private String location;
    private String note; // optional
}

