package vn.edu.kma.traceability_core_service.dto.request;
import lombok.Data;
import java.util.List;
@Data public class ReviewCreateRequest {
    private String claimToken;
    private Integer rating;
    private String content;
    private List<String> mediaIds;
}
