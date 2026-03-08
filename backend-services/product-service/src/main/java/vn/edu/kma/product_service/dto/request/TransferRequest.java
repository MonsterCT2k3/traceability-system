package vn.edu.kma.product_service.dto.request;

import lombok.Data;

@Data
public class TransferRequest {
    private String newOwnerId; // ID của người nhận
}
