package vn.edu.kma.product_service.dto.request;

import lombok.Data;

@Data
public class HistoryRequest {
    private String productId;
    private String action;      // CREATE, HARVEST, PROCESS...
    private String description; // Chi tiết hành động
    private String location;
}
