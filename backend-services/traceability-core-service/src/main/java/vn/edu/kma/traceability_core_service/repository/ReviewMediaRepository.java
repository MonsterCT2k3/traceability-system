package vn.edu.kma.traceability_core_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.kma.traceability_core_service.entity.ReviewMedia;
import java.util.List;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, String> {
    List<ReviewMedia> findByReviewIdOrderBySortOrderAsc(String reviewId);
    List<ReviewMedia> findByIdIn(List<String> ids);
}
