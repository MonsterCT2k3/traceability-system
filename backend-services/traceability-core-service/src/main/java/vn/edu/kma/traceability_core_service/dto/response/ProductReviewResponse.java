package vn.edu.kma.traceability_core_service.dto.response;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductReviewResponse {
    private String id;
    private String productId;
    private String productName;
    private String productImageUrl;
    private String productDescription;
    private String unitSerial;
    private Integer rating;
    private String content;
    private String reviewerName;
    private String reviewerAvatarUrl;
    private boolean verifiedOwnership;
    private List<ReviewMediaResponse> media;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
