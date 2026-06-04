package vn.edu.kma.traceability_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_reviews")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductReview {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private ProductUnitClaim claim;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_unit_id", nullable = false, unique = true)
    private ProductUnit productUnit;
    @Column(nullable = false, length = 36)
    private String productId;
    @Column(nullable = false, length = 36)
    private String reviewerId;
    @Column(nullable = false)
    private Integer rating;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void create() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
