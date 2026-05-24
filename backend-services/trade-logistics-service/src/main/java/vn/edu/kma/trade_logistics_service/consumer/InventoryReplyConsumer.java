package vn.edu.kma.trade_logistics_service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.common.event.InventoryReplyEvent;
import vn.edu.kma.trade_logistics_service.domain.TradeOrderStatus;
import vn.edu.kma.trade_logistics_service.entity.TradeOrder;
import vn.edu.kma.trade_logistics_service.entity.TransferRecord;
import vn.edu.kma.trade_logistics_service.repository.TradeOrderRepository;
import vn.edu.kma.trade_logistics_service.repository.TransferRecordRepository;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryReplyConsumer {

    private final TradeOrderRepository tradeOrderRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @KafkaListener(topics = "trade.inventory.replies", groupId = "trade-logistics-group")
    @Transactional
    public void handleInventoryReply(String eventJson) {
        log.info("Received InventoryReplyEvent JSON: {}", eventJson);
        InventoryReplyEvent event;
        try {
            event = objectMapper.readValue(eventJson, InventoryReplyEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse InventoryReplyEvent: {}", e.getMessage());
            return;
        }
        TradeOrder order = tradeOrderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Order not found for id: {}", event.getOrderId());
            return;
        }

        if (order.getStatus() != TradeOrderStatus.PROCESSING) {
            log.warn("Order {} is not in PROCESSING state (current: {})", order.getId(), order.getStatus());
            return;
        }

        if ("SUCCESS".equals(event.getStatus())) {
            if ("ACCEPT".equals(event.getEventType())) {
                order.setStatus(TradeOrderStatus.ACCEPTED);
            } else if ("DELIVER".equals(event.getEventType())) {
                order.setStatus(TradeOrderStatus.DELIVERED);
                
                // Create TransferRecords for delivered cartons
                if (event.getCartonIds() != null) {
                    for (String cartonId : event.getCartonIds()) {
                        TransferRecord tr = TransferRecord.builder()
                                .targetType("CARTON")
                                .targetId(cartonId)
                                .fromUserId(order.getSellerId())
                                .toUserId(order.getBuyerId())
                                .status("ACCEPTED")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .blockchainStatus("SKIPPED")
                                .build();
                        transferRecordRepository.save(tr);
                    }
                }
            }
        } else {
            // Failed
            if ("ACCEPT".equals(event.getEventType())) {
                order.setStatus(TradeOrderStatus.PENDING); // Rollback to pending so they can try again or cancel
            } else if ("DELIVER".equals(event.getEventType())) {
                order.setStatus(TradeOrderStatus.ERROR); // Delivery failed
            }
            log.error("Order processing failed for order {}: {}", order.getId(), event.getErrorMessage());
        }

        tradeOrderRepository.save(order);

        // Notify User via WebSocket
        // For simplicity, we notify the seller. Or both.
        String destinationSeller = "/topic/user/" + order.getSellerId() + "/orders";
        String destinationBuyer = "/topic/user/" + order.getBuyerId() + "/orders";
        messagingTemplate.convertAndSend(destinationSeller, event);
        messagingTemplate.convertAndSend(destinationBuyer, event);
    }
}
