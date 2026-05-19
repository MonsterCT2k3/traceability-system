package vn.edu.kma.product_service.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ScanHistoryResponse {
    private String id;
    private String unitSerial;
    private LocalDateTime scannedAt;
    
    // Additional data fetched from ProductUnit & Product
    private String productName;
    private String productImage;
}
