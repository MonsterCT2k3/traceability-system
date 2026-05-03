package vn.edu.kma.product_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceHistoryEvent {
    private String eventType; // RAW_BATCH_CREATED, PALLET_MANUFACTURED, TRANSFER_ACCEPTED
    private String eventDescription;
    private LocalDateTime timestamp;
    private String actorId; // ID người thực hiện / người nhận (Retailer, Wholesaler)
    private String actorName; // Tên hiển thị của người/doanh nghiệp
    private String location;
    private String txHash; // Nếu có ghi nhận trên blockchain
    private Boolean isVerifiedOnChain; // true: đã đối chiếu khớp với blockchain, false: sai lệch, null: chưa kiểm tra
}
