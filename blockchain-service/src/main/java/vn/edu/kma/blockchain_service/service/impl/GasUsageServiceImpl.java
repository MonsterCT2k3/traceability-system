package vn.edu.kma.blockchain_service.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.blockchain_service.domain.GasOperation;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;
import vn.edu.kma.blockchain_service.dto.BlockchainExecutionResult;
import vn.edu.kma.blockchain_service.dto.GasUsageRequestMetadata;
import vn.edu.kma.blockchain_service.dto.response.GasUsageBreakdownResponse;
import vn.edu.kma.blockchain_service.dto.response.GasUsageSummaryResponse;
import vn.edu.kma.blockchain_service.dto.response.GasUsageTransactionResponse;
import vn.edu.kma.blockchain_service.entity.BlockchainGasUsage;
import vn.edu.kma.blockchain_service.repository.BlockchainGasUsageRepository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GasUsageServiceImpl implements vn.edu.kma.blockchain_service.service.GasUsageService {

    private static final List<String> BILLABLE_ROLES = List.of("SUPPLIER", "MANUFACTURER");

    private final BlockchainGasUsageRepository repository;

    @Override
    @Transactional
    public BlockchainGasUsage start(GasUsageRequestMetadata metadata) {
        validateMetadata(metadata);
        Optional<BlockchainGasUsage> existing = repository.findByRequestId(metadata.getRequestId());
        if (existing.isPresent()) {
            return existing.get();
        }

        return repository.save(BlockchainGasUsage.builder()
                .requestId(metadata.getRequestId())
                .operation(metadata.getOperation())
                .entityId(metadata.getEntityId())
                .entityType(metadata.getEntityType())
                .sourceService(metadata.getSourceService())
                .billingActorId(metadata.getBillingActorId())
                .billingRole(normalizeRole(metadata.getBillingRole()))
                .initiatedByUserId(blankToNull(metadata.getInitiatedByUserId()))
                .status(GasUsageStatus.PENDING)
                .submittedAt(Instant.now())
                .build());
    }

    @Override
    @Transactional
    public BlockchainGasUsage complete(String requestId, BlockchainExecutionResult result) {
        BlockchainGasUsage usage = repository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Gas usage request not found: " + requestId));
        if (isTerminal(usage)) {
            return usage;
        }

        usage.setStatus(result.getStatus());
        usage.setTxHash(blankToNull(result.getTxHash()));
        usage.setGasUsed(result.getGasUsed());
        usage.setEffectiveGasPriceWei(result.getEffectiveGasPriceWei());
        usage.setFeeWei(result.getFeeWei());
        usage.setBlockNumber(result.getBlockNumber());
        usage.setSubmittedAt(result.getSubmittedAt() == null ? usage.getSubmittedAt() : result.getSubmittedAt());
        usage.setMinedAt(result.getMinedAt());
        usage.setErrorCode(blankToNull(result.getErrorCode()));
        usage.setErrorMessage(blankToNull(result.getErrorMessage()));
        return repository.save(usage);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlockchainGasUsage> findByRequestId(String requestId) {
        return repository.findByRequestId(requestId);
    }

    @Override
    public boolean isTerminal(BlockchainGasUsage usage) {
        return usage.getStatus() == GasUsageStatus.SUCCESS
                || usage.getStatus() == GasUsageStatus.FAILED_ON_CHAIN
                || usage.getStatus() == GasUsageStatus.SUBMISSION_FAILED;
    }

    @Override
    @Transactional(readOnly = true)
    public GasUsageSummaryResponse summary(String actorId, String role, Instant from, Instant to) {
        List<BlockchainGasUsage> usages = repository.findAll(
                specification(actorId, role, null, null, from, to));

        BigInteger actualFeeWei = BigInteger.ZERO;
        BigInteger successFeeWei = BigInteger.ZERO;
        BigInteger failedOnChainFeeWei = BigInteger.ZERO;
        long successCount = 0;
        long failedOnChainCount = 0;
        long submissionFailedCount = 0;
        long receiptUnknownCount = 0;

        java.util.Map<String, BreakdownAccumulator> breakdownMap = new java.util.LinkedHashMap<>();
        for (BlockchainGasUsage usage : usages) {
            BigInteger fee = usage.getFeeWei() == null ? BigInteger.ZERO : usage.getFeeWei();
            if (usage.getStatus() == GasUsageStatus.SUCCESS || usage.getStatus() == GasUsageStatus.FAILED_ON_CHAIN) {
                actualFeeWei = actualFeeWei.add(fee);
            }
            if (usage.getStatus() == GasUsageStatus.SUCCESS) {
                successFeeWei = successFeeWei.add(fee);
                successCount++;
            } else if (usage.getStatus() == GasUsageStatus.FAILED_ON_CHAIN) {
                failedOnChainFeeWei = failedOnChainFeeWei.add(fee);
                failedOnChainCount++;
            } else if (usage.getStatus() == GasUsageStatus.SUBMISSION_FAILED) {
                submissionFailedCount++;
            } else if (usage.getStatus() == GasUsageStatus.RECEIPT_UNKNOWN) {
                receiptUnknownCount++;
            }

            String key = usage.getOperation().name() + "|" + usage.getStatus().name();
            breakdownMap.computeIfAbsent(key, ignored -> new BreakdownAccumulator(usage.getOperation().name(), usage.getStatus().name()))
                    .add(fee);
        }

        List<GasUsageBreakdownResponse> breakdown = breakdownMap.values().stream()
                .map(item -> GasUsageBreakdownResponse.builder()
                        .operation(item.operation)
                        .status(item.status)
                        .count(item.count)
                        .feeWei(item.feeWei.toString())
                        .build())
                .toList();

        return GasUsageSummaryResponse.builder()
                .actualFeeWei(actualFeeWei.toString())
                .successFeeWei(successFeeWei.toString())
                .failedOnChainFeeWei(failedOnChainFeeWei.toString())
                .successCount(successCount)
                .failedOnChainCount(failedOnChainCount)
                .submissionFailedCount(submissionFailedCount)
                .receiptUnknownCount(receiptUnknownCount)
                .totalCount(usages.size())
                .breakdown(breakdown)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GasUsageTransactionResponse> transactions(
            String actorId,
            String role,
            GasOperation operation,
            GasUsageStatus status,
            Instant from,
            Instant to,
            Pageable pageable) {
        return repository.findAll(specification(actorId, role, operation, status, from, to), pageable)
                .map(this::toResponse);
    }

    private Specification<BlockchainGasUsage> specification(
            String actorId,
            String role,
            GasOperation operation,
            GasUsageStatus status,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actorId != null && !actorId.isBlank()) {
                predicates.add(cb.equal(root.get("billingActorId"), actorId.trim()));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(root.get("billingRole"), role.trim().toUpperCase()));
            }
            if (operation != null) {
                predicates.add(cb.equal(root.get("operation"), operation));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateMetadata(GasUsageRequestMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("gas metadata must not be null");
        }
        requireNonBlank(metadata.getRequestId(), "requestId");
        requireNonBlank(metadata.getEntityId(), "entityId");
        requireNonBlank(metadata.getEntityType(), "entityType");
        requireNonBlank(metadata.getBillingActorId(), "billingActorId");
        if (metadata.getOperation() == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        String role = normalizeRole(metadata.getBillingRole());
        if (!BILLABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("billingRole must be SUPPLIER or MANUFACTURER");
        }
    }

    private GasUsageTransactionResponse toResponse(BlockchainGasUsage usage) {
        return GasUsageTransactionResponse.builder()
                .id(usage.getId().toString())
                .requestId(usage.getRequestId())
                .txHash(usage.getTxHash())
                .operation(usage.getOperation().name())
                .entityId(usage.getEntityId())
                .entityType(usage.getEntityType())
                .sourceService(usage.getSourceService())
                .billingActorId(usage.getBillingActorId())
                .billingRole(usage.getBillingRole())
                .initiatedByUserId(usage.getInitiatedByUserId())
                .status(usage.getStatus().name())
                .gasUsed(toStringOrNull(usage.getGasUsed()))
                .effectiveGasPriceWei(toStringOrNull(usage.getEffectiveGasPriceWei()))
                .feeWei(toStringOrNull(usage.getFeeWei()))
                .blockNumber(toStringOrNull(usage.getBlockNumber()))
                .errorCode(usage.getErrorCode())
                .errorMessage(usage.getErrorMessage())
                .submittedAt(usage.getSubmittedAt())
                .minedAt(usage.getMinedAt())
                .createdAt(usage.getCreatedAt())
                .updatedAt(usage.getUpdatedAt())
                .build();
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String toStringOrNull(BigInteger value) {
        return value == null ? null : value.toString();
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    private static BigInteger asBigInteger(Object value) {
        if (value == null) {
            return BigInteger.ZERO;
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.toBigInteger();
        }
        if (value instanceof Number number) {
            return BigInteger.valueOf(number.longValue());
        }
        return new BigInteger(value.toString());
    }

    private static class BreakdownAccumulator {
        private final String operation;
        private final String status;
        private long count;
        private BigInteger feeWei = BigInteger.ZERO;

        private BreakdownAccumulator(String operation, String status) {
            this.operation = operation;
            this.status = status;
        }

        private BreakdownAccumulator add(BigInteger fee) {
            count++;
            feeWei = feeWei.add(fee == null ? BigInteger.ZERO : fee);
            return this;
        }
    }
}
