package vn.edu.kma.product_service.entity;

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

    @Column(nullable = false)
    private String palletId;

    /**
     * Denormalize theo pallet để truy vấn nhanh theo catalog.
     */
    @Column(nullable = false)
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

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
