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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carton_id", nullable = false)
    private Carton carton;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id", nullable = false)
    private Pallet pallet;

    @Column(nullable = false, length = 36)
    private String productId;

    /**
     * Mã serial in trên bao bì / dùng cho trace (duy nhất toàn hệ thống).
     */
    @Column(nullable = false, unique = true)
    private String unitSerial;

    /** Số lần quét truy xuất công khai (theo id/serial). */
    @Column(nullable = false)
    @Builder.Default
    private Integer scanCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String ownerId;

    private String manufacturerId; // ID nhà sản xuất ban đầu (không đổi)

    @Builder.Default
    private String status = "IN_STOCK"; // IN_STOCK, SHIPPING, DELIVERED

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (scanCount == null) {
            scanCount = 0;
        }
    }

    // Convenience helpers
    public String getCartonId() {
        return carton != null ? carton.getId() : null;
    }

    public String getPalletId() {
        return pallet != null ? pallet.getId() : null;
    }

    public String getProductId() {
        return productId;
    }
}
