package vn.edu.kma.blockchain_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyHashesResponse {
    private List<VerifyResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyResult {
        private String batchIdHex;
        private boolean isMatch;
    }
}
