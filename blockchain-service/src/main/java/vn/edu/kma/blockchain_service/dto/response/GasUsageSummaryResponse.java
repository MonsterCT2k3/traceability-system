package vn.edu.kma.blockchain_service.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GasUsageSummaryResponse {
    String actualFeeWei;
    String successFeeWei;
    String failedOnChainFeeWei;
    long successCount;
    long failedOnChainCount;
    long submissionFailedCount;
    long receiptUnknownCount;
    long totalCount;
    List<GasUsageBreakdownResponse> breakdown;
}
