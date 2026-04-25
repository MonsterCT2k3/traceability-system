package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dữ liệu truy xuất công khai (không lộ secret, không lộ ownerId).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnitPublicTraceResponse {

    private String unitId;
    private String unitSerial;

    private String productId;
    private String productName;
    private String productDescription;
    private String productImageUrl;

    private String cartonCode;

    private String palletCode;
    private String palletName;
    private String palletManufacturedAt;
    private String palletExpiryAt;

    /** Số lần quét truy xuất công khai (theo id/serial). */
    private Integer scanCount;
}
