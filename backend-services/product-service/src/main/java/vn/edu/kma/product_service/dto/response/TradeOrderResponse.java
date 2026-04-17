package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.kma.product_service.domain.OrderType;
import vn.edu.kma.product_service.domain.TradeOrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderResponse {
    private String id;
    private String orderCode;
    private OrderType orderType;
    private String buyerId;
    private String sellerId;
    private TradeOrderStatus status;
    private String carrierId;
    private String note;
    private String deliveryTxHash;
    private String deliveryChainStatus;
    private String deliveryChainError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TradeOrderLineResponse> lines;
}
