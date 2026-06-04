package vn.edu.kma.traceability_core_service.dto.response;
import lombok.*;
import java.util.Map;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewSummaryResponse {
    private long totalReviews;
    private double averageRating;
    private Map<Integer, Long> ratingCounts;
}
