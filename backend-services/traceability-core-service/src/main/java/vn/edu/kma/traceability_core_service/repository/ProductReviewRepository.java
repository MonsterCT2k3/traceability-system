package vn.edu.kma.traceability_core_service.repository;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.traceability_core_service.entity.ProductReview;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, String> {
    Optional<ProductReview> findByClaimId(String claimId);
    Page<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(String productId, String status, Pageable pageable);
    Page<ProductReview> findByReviewerIdOrderByCreatedAtDesc(String reviewerId, Pageable pageable);
}
