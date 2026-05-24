package vn.edu.kma.traceability_core_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyHashesRequest {
    private List<HashItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HashItem {
        private String batchIdHex;
        private String dataHashHex;
        private String type; // RAW or TRANSFORMED
    }
}

