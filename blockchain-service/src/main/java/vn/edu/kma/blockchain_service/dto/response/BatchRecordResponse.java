// blockchain-service/src/main/java/vn/edu/kma/blockchain_service/dto/response/BatchRecordResponse.java
package vn.edu.kma.blockchain_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchRecordResponse {
    private String batchIdHex;   // để trả lại cho tiện
    private String dataHashHex;
    private String actor;        // địa chỉ ví
    private long timestamp;
}
