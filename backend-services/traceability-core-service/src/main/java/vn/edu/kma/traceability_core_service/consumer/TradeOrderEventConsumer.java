package vn.edu.kma.traceability_core_service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.kma.common.event.InventoryReplyEvent;
import vn.edu.kma.common.event.TradeOrderEvent;
import vn.edu.kma.traceability_core_service.entity.Carton;
import vn.edu.kma.traceability_core_service.repository.CartonRepository;
import vn.edu.kma.traceability_core_service.repository.ProductUnitRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class TradeOrderEventConsumer {

    private final CartonRepository cartonRepository;
    private final ProductUnitRepository productUnitRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @KafkaListener(topics = "trade.order.events", groupId = "product-group")
    @Transactional
    public void handleTradeOrderEvent(String eventJson) {
        log.info("Received TradeOrderEvent JSON: {}", eventJson);
        TradeOrderEvent event;
        try {
            event = objectMapper.readValue(eventJson, TradeOrderEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse TradeOrderEvent: {}", e.getMessage());
            return;
        }
        try {
            List<String> processedCartonIds = new ArrayList<>();
            
            if ("ACCEPT".equals(event.getEventType())) {
                for (TradeOrderEvent.OrderItem item : event.getItems()) {
                    processShipCartons(event.getSellerId(), item.getProductId(), item.getQuantity(), processedCartonIds);
                }
            } else if ("DELIVER".equals(event.getEventType())) {
                for (TradeOrderEvent.OrderItem item : event.getItems()) {
                    processDeliverCartons(event.getSellerId(), event.getBuyerId(), item.getProductId(), item.getQuantity(), processedCartonIds);
                }
            } else {
                throw new IllegalArgumentException("Unknown eventType: " + event.getEventType());
            }

            // Reply Success
            InventoryReplyEvent reply = InventoryReplyEvent.builder()
                    .orderId(event.getOrderId())
                    .status("SUCCESS")
                    .cartonIds(processedCartonIds)
                    .eventType(event.getEventType())
                    .build();
            kafkaTemplate.send("trade.inventory.replies", reply);

        } catch (Exception e) {
            log.error("Failed to process TradeOrderEvent for orderId {}: {}", event.getOrderId(), e.getMessage());
            // Reply Failed
            InventoryReplyEvent reply = InventoryReplyEvent.builder()
                    .orderId(event.getOrderId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .eventType(event.getEventType())
                    .build();
            kafkaTemplate.send("trade.inventory.replies", reply);
        }
    }

    private void processShipCartons(String sellerId, String productId, int quantity, List<String> cartonIds) {
        List<Carton> cartons = cartonRepository.findAvailableForShipping(productId, sellerId, org.springframework.data.domain.PageRequest.of(0, quantity));
        if (cartons.size() < quantity) {
            throw new RuntimeException("Sản phẩm " + productId + ": Không đủ số lượng thùng hàng sẵn sàng trong kho!");
        }
        for (Carton c : cartons) {
            c.setStatus("SHIPPING");
            cartonRepository.save(c);
            productUnitRepository.updateStatusByCartonId(c.getId(), "SHIPPING");
            cartonIds.add(c.getId());
        }
    }

    private void processDeliverCartons(String sellerId, String buyerId, String productId, int quantity, List<String> cartonIds) {
        List<Carton> cartons = cartonRepository.findAvailableForDelivery(
                productId, sellerId, org.springframework.data.domain.PageRequest.of(0, quantity));

        if (cartons.size() < quantity) {
            cartons = cartonRepository.findAvailableForShipping(
                    productId, sellerId, org.springframework.data.domain.PageRequest.of(0, quantity));
        }

        if (cartons.size() < quantity) {
            throw new RuntimeException("Sản phẩm " + productId + ": không đủ tồn kho hoặc hàng chưa sẵn sàng vận chuyển");
        }

        for (Carton c : cartons) {
            c.setStatus("DELIVERED");
            c.setOwnerId(buyerId);
            cartonRepository.save(c);
            productUnitRepository.updateOwnerAndStatusByCartonId(c.getId(), buyerId, "DELIVERED");
            cartonIds.add(c.getId());
        }
    }
}
