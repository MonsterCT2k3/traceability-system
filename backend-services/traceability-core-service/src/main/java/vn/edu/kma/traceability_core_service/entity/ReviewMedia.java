package vn.edu.kma.traceability_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_media")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewMedia {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private ProductReview review;
    @Column(nullable = false, length = 36)
    private String uploaderId;
    @Column(nullable = false, length = 10)
    private String mediaType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mediaUrl;
    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;
    @Column(nullable = false)
    private String cloudinaryPublicId;
    @Column(nullable = false)
    private Long fileSize;
    private Integer durationSeconds;
    @Column(nullable = false)
    private Integer sortOrder;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime attachedAt;
    @PrePersist void create() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
