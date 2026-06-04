package vn.edu.kma.traceability_core_service.dto.request;

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

    /**
     * Danh sách input tổng quát. Nếu có dữ liệu, field này được ưu tiên hơn
     * parentRawBatchIdHexes để giữ tương thích API cũ.
     */
    private List<PalletInputRequest> inputs;
}


