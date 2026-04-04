// blockchain-service/src/main/java/vn/edu/kma/blockchain_service/dto/response/TransformedBatchRecordResponse.java
package vn.edu.kma.blockchain_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransformedBatchRecordResponse {
    private String batchIdHex;
    private String dataHashHex;
    private String parentRootHex;
    private String actor;
    private long timestamp;
}
