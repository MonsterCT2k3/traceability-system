package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PalletBulkPackingResponse {
    private String palletId;
    private String palletCode;
    private int cartonsCreated;
    private int unitsCreated;
    private List<PalletBulkCartonResult> cartons;
}
