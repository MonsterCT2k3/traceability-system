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

    /** Số lần quét truy xuất công khai (theo id/serial). */
    @Column(nullable = false)
    @Builder.Default
    private Integer scanCount = 0;

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
