package vn.edu.kma.product_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scan_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String unitSerial;

    @Column(nullable = false)
    private LocalDateTime scannedAt;

    @PrePersist
    protected void onCreate() {
        if (scannedAt == null) {
            scannedAt = LocalDateTime.now();
        }
    }
}
