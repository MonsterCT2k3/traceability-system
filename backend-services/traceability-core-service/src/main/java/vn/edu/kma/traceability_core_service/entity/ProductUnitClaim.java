package vn.edu.kma.traceability_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_unit_claims")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductUnitClaim {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_unit_id", nullable = false, unique = true)
    private ProductUnit productUnit;
    @Column(nullable = false, unique = true, length = 64)
    private String claimTokenHash;
    @Column(nullable = false, length = 20)
    private String status;
    private String claimedByUserId;
    private LocalDateTime claimedAt;
    private LocalDateTime revokedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
