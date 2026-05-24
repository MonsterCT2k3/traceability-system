package vn.edu.kma.traceability_core_service.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_serial", referencedColumnName = "unitSerial", nullable = false)
    private ProductUnit productUnit;

    @Column(nullable = false)
    private LocalDateTime scannedAt;

    @PrePersist
    protected void onCreate() {
        if (scannedAt == null) {
            scannedAt = LocalDateTime.now();
        }
    }

    // Convenience helper
    public String getUnitSerial() {
        return productUnit != null ? productUnit.getUnitSerial() : null;
    }
}

