package com.akhilpaila.order.kafka;

import com.akhilpaila.order.dto.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-placed}")
    private String orderPlacedTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.info("Publishing order.placed event for orderId: {}",
            event.getOrderId());
        kafkaTemplate.send(orderPlacedTopic,
            event.getOrderId().toString(), event);
        log.info("Event published to topic: {}", orderPlacedTopic);
    }
}
