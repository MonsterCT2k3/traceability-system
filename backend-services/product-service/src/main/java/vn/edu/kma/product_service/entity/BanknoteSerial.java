package vn.edu.kma.product_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Một số seri in trên tờ tiền (polymer) do NSX đăng ký — lưu từng seri một dòng.
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
}
