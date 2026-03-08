package vn.edu.kma.blockchain_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainHistoryResponse {
    private String productId;
    private String action;
    private String description;
    private long timestamp;
    private String actor; // Địa chỉ ví người thực hiện
}