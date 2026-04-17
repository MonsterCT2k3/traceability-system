package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.kma.product_service.domain.OrderType;
import vn.edu.kma.product_service.domain.TradeOrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trade_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 48)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OrderType orderType;

    /** userId người đặt (MANUFACTURER hoặc RETAILER). */
    @Column(nullable = false, length = 36)
    private String buyerId;

    /** userId người bán (SUPPLIER hoặc MANUFACTURER). */
    @Column(nullable = false, length = 36)
    private String sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TradeOrderStatus status;

    /** userId TRANSPORTER — không đổi quyền sở hữu sang VC, chỉ giao nhiệm vụ. */
    @Column(length = 36)
    private String carrierId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(length = 66)
    private String deliveryTxHash;

    @Column(length = 32)
    private String deliveryChainStatus;

    @Column(columnDefinition = "TEXT")
    private String deliveryChainError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TradeOrderLine> lines = new ArrayList<>();

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
