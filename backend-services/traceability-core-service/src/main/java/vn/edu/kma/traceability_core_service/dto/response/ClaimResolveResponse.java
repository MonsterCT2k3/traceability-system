package vn.edu.kma.traceability_core_service.dto.response;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClaimResolveResponse {
    private String claimStatus;
    private String productUnitId;
    private String unitSerial;
    private String productId;
    private String productName;
    private String productImageUrl;
    private ProductReviewResponse existingReview;
}
