package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Một số seri in trên tờ tiền (polymer) do NSX đăng ký — lưu từng seri một dòng.
 * Quan hệ 1-1 với ProductUnit thông qua serial_value ↔ unit_serial.
 */
@Entity
@Table(
        name = "banknote_serial",
        uniqueConstraints = @UniqueConstraint(name = "uk_banknote_serial_value", columnNames = "serial_value")
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BanknoteSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "serial_value", nullable = false, length = 32)
    private String serialValue;

    @Column(name = "registered_by_user_id", nullable = false, length = 36)
    private String registeredByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Đánh dấu serial này đã được gán cho 1 đơn vị sản phẩm.
     * Mặc định là false (chưa sử dụng).
     */
    @Builder.Default
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    /**
     * FK ngược trỏ về product_units.unit_serial.
     * Khi serial được gán vào Product Unit, cột này sẽ được cập nhật.
     * Quan hệ 1-1: 1 serial chỉ được gán cho đúng 1 ProductUnit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "assigned_unit_serial",
            referencedColumnName = "unitSerial",
            foreignKey = @ForeignKey(name = "fk_banknote_serial_product_unit"),
            nullable = true
    )
    private ProductUnit assignedUnit;
}
