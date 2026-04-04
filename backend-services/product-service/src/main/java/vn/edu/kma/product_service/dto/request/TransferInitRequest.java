package vn.edu.kma.product_service.dto.request;

import lombok.Data;

@Data
public class TransferInitRequest {
    /**
     * Loại đối tượng chuyển giao: PALLET | RAW_BATCH
     */
    private String targetType;

    /**
     * ID của bản ghi trong DB theo targetType.
     */
    private String targetId;

    /**
     * ID người nhận.
     */
    private String newOwnerId;
}
