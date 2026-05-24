package vn.edu.kma.trade_logistics_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.edu.kma.common.dto.response.ApiResponse;

import java.util.Map;

@FeignClient(name = "catalog-service", url = "http://localhost:8084")
public interface CatalogClient {

    @GetMapping("/api/v1/products/{id}")
    ApiResponse<Map<String, Object>> getProductById(@PathVariable("id") String id);

    @GetMapping("/api/v1/material-catalog/items/{id}")
    ApiResponse<Map<String, Object>> getMaterialItemById(@PathVariable("id") String id);
}
