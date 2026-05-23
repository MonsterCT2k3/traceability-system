package vn.edu.kma.blockchain_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainOwnershipChangeEvent implements Serializable {
    private String entityId;
    private String entityType; // "TRANSFER", "TRADE_ORDER"
    private String batchIdHex;
    private String fromUserId;
    private String toUserId;
}
