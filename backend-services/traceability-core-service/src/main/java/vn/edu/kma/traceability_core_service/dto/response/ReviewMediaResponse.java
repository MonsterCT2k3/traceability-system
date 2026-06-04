package vn.edu.kma.traceability_core_service.dto.response;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewMediaResponse {
    private String id;
    private String mediaType;
    private String mediaUrl;
    private String thumbnailUrl;
}
