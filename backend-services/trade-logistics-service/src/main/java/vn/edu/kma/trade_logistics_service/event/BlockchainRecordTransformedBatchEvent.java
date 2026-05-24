package vn.edu.kma.trade_logistics_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainRecordTransformedBatchEvent implements Serializable {
    private String entityId;
    private String entityType; // "PALLET", "PRODUCT_UNIT", vv.
    private String batchIdHex;
    private String dataHashHex;
    private List<String> parentHashesHex;
}
