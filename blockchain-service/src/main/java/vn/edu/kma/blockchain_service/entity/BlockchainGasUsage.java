package vn.edu.kma.blockchain_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.kma.blockchain_service.domain.GasOperation;
import vn.edu.kma.blockchain_service.domain.GasUsageStatus;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blockchain_gas_usage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainGasUsage {
    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "tx_hash", unique = true)
    private String txHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false)
    private GasOperation operation;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "source_service", nullable = false)
    private String sourceService;

    @Column(name = "billing_actor_id", nullable = false)
    private String billingActorId;

    @Column(name = "billing_role", nullable = false)
    private String billingRole;

    @Column(name = "initiated_by_user_id")
    private String initiatedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GasUsageStatus status;

    @Column(name = "gas_used", precision = 78)
    private BigInteger gasUsed;

    @Column(name = "effective_gas_price_wei", precision = 78)
    private BigInteger effectiveGasPriceWei;

    @Column(name = "fee_wei", precision = 78)
    private BigInteger feeWei;

    @Column(name = "block_number", precision = 78)
    private BigInteger blockNumber;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "mined_at")
    private Instant minedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
