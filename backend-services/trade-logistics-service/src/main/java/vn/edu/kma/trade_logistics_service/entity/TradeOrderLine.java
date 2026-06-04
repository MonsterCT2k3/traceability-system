package vn.edu.kma.trade_logistics_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trade_order_lines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private TradeOrder order;

    @Column(nullable = false)
    private Integer lineIndex;

    /** MANUFACTURER_TO_SUPPLIER: lô nguyên liệu NCC. */
    @Column(length = 36)
    private String targetRawBatchId;

    /** MANUFACTURER_TO_MANUFACTURER: pallet cụ thể được mua làm input sản xuất. */
    @Column(length = 36)
    private String targetPalletId;

    /** Số lượng đặt (chuỗi, khớp RawBatch.quantity). */
    @Column(length = 64)
    private String quantityRequested;

    @Column(length = 32)
    private String unit;

    /** RETAILER_TO_MANUFACTURER: sản phẩm của NSX. */
    @Column(length = 36)
    private String productId;

    /** Số thùng (≥ 1). */
    private Integer quantityCartons;
}
