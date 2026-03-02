package vn.edu.kma.product_service.service;

import vn.edu.kma.product_service.dto.request.HistoryRequest;
import vn.edu.kma.product_service.entity.ProductHistory;

import java.util.List;

public interface ProductHistoryService {
    ProductHistory createHistory(HistoryRequest request, String token);
    List<ProductHistory> getHistoryByProductId(String productId);
}
