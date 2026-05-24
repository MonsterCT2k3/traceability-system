package vn.edu.kma.trade_logistics_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainReplyEvent implements Serializable {
    private String entityId;
    private String entityType;
    private String txHash;
    private String status; // "SUCCESS" or "ERROR"
    private String errorMessage;
}
