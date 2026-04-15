package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String rawBatchCode;

    @Column(nullable = false)
    private String materialType;

    @Column(nullable = false)
    private String materialName;

    @Column(nullable = false)
    private String harvestedAt;

    @Column(nullable = false)
    private String quantity;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private String schemaVersion;

    @Column(nullable = false, length = 66, unique = true)
    private String batchIdHex; // bytes32 hex (0x + 64)

    @Column(nullable = false, length = 66)
    private String dataHashHex; // keccak256(payload) hex (0x + 64)

    @Column(nullable = true, length = 66)
    private String anchorTxHash;

    /**
     * Người tạo lô nguyên liệu (lấy từ token).
     * Lưu để hiển thị/trace DB, on-chain không lưu owner mapping riêng.
     */
    @Column(nullable = true)
    private String actorId;

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'NOT_SHIPPED'")
    private String status; // NOT_SHIPPED, PENDING_SHIPMENT, SHIPPED

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

