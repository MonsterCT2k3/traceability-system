package vn.edu.kma.product_service.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.edu.kma.common.dto.response.ApiResponse;

@FeignClient(name = "identity-service", url = "${app.services.identity.url:http://localhost:8081}")
public interface IdentityClient {

    @GetMapping("/api/v1/users/directory/by-id/{userId}")
    ApiResponse<java.util.Map<String, Object>> getUserById(@PathVariable("userId") String userId);
}
