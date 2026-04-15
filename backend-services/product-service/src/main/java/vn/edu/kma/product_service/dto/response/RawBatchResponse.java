package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawBatchResponse {
    private String id;
    private String rawBatchCode;
    private String materialType;
    private String materialName;
    private String harvestedAt;
    private String quantity;
    private String unit;
    private String location;
    private String note;
    private String status;
    private String batchIdHex;
    private String anchorTxHash;
    private LocalDateTime createdAt;
}
