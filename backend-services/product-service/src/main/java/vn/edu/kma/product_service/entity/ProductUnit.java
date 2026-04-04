package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String cartonId;

    @Column(nullable = false)
    private String palletId;

    @Column(nullable = false)
    private String productId;

    /**
     * Mã serial in trên bao bì / dùng cho trace (duy nhất toàn hệ thống).
     */
    @Column(nullable = false, unique = true)
    private String unitSerial;

    /**
     * Chủ sau khi claim; null = chưa gán.
     */
    private String ownerId;

    /**
     * keccak256/SHA-256 hex (64 ký tự, không prefix 0x) của secret scratch-off.
     * Null nếu chưa sinh secret.
     */
    @Column(length = 64, unique = true)
    private String secretHash;

    /**
     * Số lần quét QR truy xuất công khai.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer scanCount = 0;

    /**
     * Thời điểm claim thành công (nếu có).
     */
    private LocalDateTime claimedAt;

    @Column(columnDefinition = "TEXT")
    private String ownerNameSnapshot;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (scanCount == null) {
            scanCount = 0;
        }
    }
}
