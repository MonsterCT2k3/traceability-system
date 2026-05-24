package vn.edu.kma.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderEvent {
    private String orderId;
    private String sellerId;
    private String buyerId;
    private List<OrderItem> items;
    private String eventType; // "ACCEPT", "DELIVER"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private int quantity;
    }
}
