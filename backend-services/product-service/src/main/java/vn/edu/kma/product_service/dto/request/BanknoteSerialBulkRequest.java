package vn.edu.kma.product_service.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BanknoteSerialBulkRequest {
    /** Danh sách seri (mobile gom theo tập trên UI; server lưu từng dòng). */
    private List<String> serials = new ArrayList<>();
}
