package vn.edu.kma.blockchain_service.dto;

import lombok.Builder;
import lombok.Value;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;

import java.math.BigInteger;
import java.time.Instant;

@Value
@Builder
public class BlockchainExecutionResult {
    String txHash;
    GasUsageStatus status;
    BigInteger gasUsed;
    BigInteger effectiveGasPriceWei;
    BigInteger feeWei;
    BigInteger blockNumber;
    Instant submittedAt;
    Instant minedAt;
    String errorCode;
    String errorMessage;
}
