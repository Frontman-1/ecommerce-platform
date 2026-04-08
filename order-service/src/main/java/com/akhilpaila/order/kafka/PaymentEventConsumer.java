package com.akhilpaila.order.kafka;

import com.akhilpaila.order.dto.PaymentProcessedEvent;
import com.akhilpaila.order.entity.Order;
import com.akhilpaila.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;

    public PaymentEventConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "payment.processed",
        groupId = "order-group",
        properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=com.akhilpaila.order.dto.PaymentProcessedEvent"
        })
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment.processed for orderId: {}",
            event.getOrderId());
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            if ("SUCCESS".equals(event.getStatus())) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
            } else {
                order.setStatus(Order.OrderStatus.FAILED);
            }
            orderRepository.save(order);
            log.info("Order {} updated to: {}",
                event.getOrderId(), order.getStatus());
        });
    }
}
