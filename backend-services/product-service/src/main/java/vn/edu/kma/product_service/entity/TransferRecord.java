package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = true)
    private String productId;

    /**
     * Pallet cụ thể được chuyển giao.
     * Có thể null với dữ liệu legacy cũ trước khi bổ sung field này.
     */
    @Column(name = "pallet_id")
    private String palletId;

    /**
     * RawBatch cụ thể được chuyển giao.
     */
    @Column(name = "raw_batch_id")
    private String rawBatchId;

    /**
     * Loại đối tượng chuyển giao: PALLET | RAW_BATCH
     */
    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    /**
     * ID bản ghi theo target_type.
     */
    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(nullable = false)
    private String fromUserId;     // Người gửi (Chủ hiện tại)

    @Column(nullable = false)
    private String toUserId;       // Người nhận

    @Column(nullable = false)
    private String status;         // PENDING, ACCEPTED, REJECTED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Tx hash sau khi gọi logOwnershipChange on-chain (0x + 64 hex). */
    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    /** Trạng thái ghi chain: ví dụ PENDING, OK, FAILED; null = chưa gọi / không áp dụng. */
    @Column(name = "blockchain_status", length = 32)
    private String blockchainStatus;

    /** Thông báo lỗi nếu gọi blockchain thất bại. */
    @Column(name = "blockchain_error", columnDefinition = "TEXT")
    private String blockchainError;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = "PENDING";
    }
}