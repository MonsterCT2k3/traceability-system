package vn.edu.kma.traceability_core_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCarrierRequest {
    /** userId TRANSPORTER */
    private String carrierId;
}

