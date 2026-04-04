package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String palletCode;

    @Column(nullable = false)
    private String palletName;

    @Column(nullable = false)
    private String batchNo;

    @Column(nullable = false)
    private String manufacturedAt;

    private String expiryAt;

    @Column(nullable = false)
    private String quantity;

    @Column(nullable = false)
    private String unit;

    private String packagingType;

    private String processingMethod;

    private String plantCode;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private String schemaVersion;

    @Column(nullable = false)
    private String actorId;

    @Column(nullable = false)
    private String ownerId;

    /**
     * bytes32 hex dùng để gọi recordTransformedBatch + ownership-change trên chain.
     */
    @Column(nullable = false, length = 66, unique = true)
    private String chainBatchIdHex;

    @Column(nullable = false, length = 66)
    private String dataHashHex;

    @Column(nullable = true, length = 66)
    private String anchorTxHash;

    /**
     * Thuộc Product (1 product nhiều pallet).
     */
    @Column(nullable = false)
    private String productId;

    /**
     * Lưu mirror danh sách parent RAW batchIdHex đã sort (để audit/trace DB).
     * Format: batchIdHex1,batchIdHex2,...
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String parentRawBatchIdHexes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

