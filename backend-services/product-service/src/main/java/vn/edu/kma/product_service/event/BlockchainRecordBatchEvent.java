package vn.edu.kma.product_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainRecordBatchEvent implements Serializable {
    private String entityId;
    private String entityType; // "RAW_BATCH", "PRODUCT", vv.
    private String batchIdHex;
    private String dataHashHex;
}
