package vn.edu.kma.blockchain_service.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GasUsageBreakdownResponse {
    String operation;
    String status;
    long count;
    String feeWei;
}
