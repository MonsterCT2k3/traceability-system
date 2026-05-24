package vn.edu.kma.traceability_core_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.edu.kma.common.dto.response.ApiResponse;

import java.util.List;
import java.util.Map;

@FeignClient(name = "trade-logistics-service", url = "${spring.url.trade-logistics-service}")
public interface TradeLogisticsClient {

    @GetMapping("/api/v1/internal/transfers/target/{targetType}/{targetId}")
    ApiResponse<List<Map<String, Object>>> getTransfersByTarget(
            @PathVariable("targetType") String targetType,
            @PathVariable("targetId") String targetId
    );
}

