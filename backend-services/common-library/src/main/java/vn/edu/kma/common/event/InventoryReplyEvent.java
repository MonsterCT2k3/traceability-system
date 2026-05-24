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
public class InventoryReplyEvent {
    private String orderId;
    private String status; // "SUCCESS", "FAILED"
    private String errorMessage;
    private List<String> cartonIds;
    private String eventType; // "ACCEPT", "DELIVER"
}
