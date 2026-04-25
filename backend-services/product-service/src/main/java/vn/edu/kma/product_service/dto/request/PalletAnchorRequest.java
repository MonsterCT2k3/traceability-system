package vn.edu.kma.product_service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PalletAnchorRequest {
    private String palletName;
    private String batchNo;

    private String manufacturedAt;
    private String expiryAt;
    private String quantity;
    private String unit;
    private String packagingType;
    private String processingMethod;
    private String location;
    private String note;

    /**
     * Danh sách RAW batchIdHex cha (đúng bytes32 hex, có thể có/không 0x).
     */
    private List<String> parentRawBatchIdHexes;
}

