package vn.edu.kma.blockchain_service.domain;

public enum GasUsageStatus {
    PENDING,
    SUCCESS,
    FAILED_ON_CHAIN,
    SUBMISSION_FAILED,
    RECEIPT_UNKNOWN
}
