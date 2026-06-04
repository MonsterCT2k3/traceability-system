package vn.edu.kma.traceability_core_service.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.edu.kma.common.dto.response.ApiResponse;
import vn.edu.kma.traceability_core_service.dto.response.VerifyHashesResponse;
import vn.edu.kma.traceability_core_service.dto.request.VerifyTransformedDirectRequest;
import vn.edu.kma.traceability_core_service.dto.response.VerifyTransformedDirectResponse;

import java.util.Map;

/**
 * Feign Client để giao tiếp với blockchain-service.
 * - name: Tên service (sau này dùng Eureka sẽ map tự động).
 * - url: Tạm thời trỏ thẳng tới cấu hình cũ để không lỗi (khi nào có Eureka sẽ bỏ url).
 */
@FeignClient(name = "blockchain-service", url = "${spring.url.blockchain-service}")
public interface BlockchainClient {

    @PostMapping("/batch")
    ApiResponse<String> recordBatch(@RequestBody Map<String, String> body);

    @PostMapping("/transformed-batch")
    ApiResponse<String> recordTransformedBatch(@RequestBody Map<String, Object> body);

    @PostMapping("/ownership-change")
    ApiResponse<String> logOwnershipChange(@RequestBody Map<String, Object> body);

    @PostMapping("/verify-hashes")
    ApiResponse<VerifyHashesResponse> verifyHashes(@RequestBody vn.edu.kma.traceability_core_service.dto.request.VerifyHashesRequest body);

    @PostMapping("/verify-transformed-direct")
    ApiResponse<VerifyTransformedDirectResponse> verifyTransformedDirect(@RequestBody VerifyTransformedDirectRequest body);
}

