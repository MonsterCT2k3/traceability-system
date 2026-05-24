package vn.edu.kma.traceability_core_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cartons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carton {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id", nullable = false)
    @JsonIgnore
    private Pallet pallet;

    /**
     * Denormalize theo pallet để truy vấn nhanh theo catalog.
     */
    @Column(nullable = false, length = 36)
    private String productId;

    @Column(nullable = false, unique = true)
    private String cartonCode;

    /**
     * Số đơn vị (item) dự kiến trong thùng.
     */
    @Column(nullable = false)
    private Integer plannedUnitCount;

    /**
     * Mô tả đơn vị đếm, ví dụ "24 hộp".
     */
    private String unitLabel;

    @Column(nullable = false)
    private String ownerId;

    private String manufacturerId; // ID nhà sản xuất ban đầu (không đổi)

    @Builder.Default
    private String status = "IN_STOCK"; // IN_STOCK, SHIPPING, DELIVERED

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Convenience helpers để các Service cũ vẫn đọc được ID
    public String getPalletId() {
        return pallet != null ? pallet.getId() : null;
    }

    public String getProductId() {
        return productId;
    }
}

