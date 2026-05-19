package vn.edu.kma.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanHistoryRequest {
    @NotBlank(message = "Số serial không được để trống")
    private String unitSerial;
}
