package vn.edu.kma.blockchain_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.kma.blockchain_service.domain.GasOperation;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;
import vn.edu.kma.blockchain_service.dto.BlockchainExecutionResult;
import vn.edu.kma.blockchain_service.dto.GasUsageRequestMetadata;
import vn.edu.kma.blockchain_service.dto.response.GasUsageSummaryResponse;
import vn.edu.kma.blockchain_service.dto.response.GasUsageTransactionResponse;
import vn.edu.kma.blockchain_service.entity.BlockchainGasUsage;

import java.time.Instant;
import java.util.Optional;

public interface GasUsageService {
    BlockchainGasUsage start(GasUsageRequestMetadata metadata);

    BlockchainGasUsage complete(String requestId, BlockchainExecutionResult result);

    Optional<BlockchainGasUsage> findByRequestId(String requestId);

    boolean isTerminal(BlockchainGasUsage usage);

    GasUsageSummaryResponse summary(String actorId, String role, Instant from, Instant to);

    Page<GasUsageTransactionResponse> transactions(
            String actorId,
            String role,
            GasOperation operation,
            GasUsageStatus status,
            Instant from,
            Instant to,
            Pageable pageable);
}
