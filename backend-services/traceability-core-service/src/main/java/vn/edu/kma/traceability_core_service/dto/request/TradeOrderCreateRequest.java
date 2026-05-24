package vn.edu.kma.traceability_core_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.kma.traceability_core_service.domain.OrderType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderCreateRequest {

    private OrderType orderType;
    /** userId người bán (NCC hoặc NSX). */
    private String sellerId;
    private String note;
    private List<TradeOrderLineRequest> lines;
}

