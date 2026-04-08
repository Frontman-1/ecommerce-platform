package com.akhilpaila.payment.kafka;

import com.akhilpaila.payment.dto.OrderPlacedEvent;
import com.akhilpaila.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

    private final PaymentService paymentService;

    public OrderEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "order.placed",
        groupId = "payment-group",
        properties = {
            "spring.json.use.type.headers=false",
            "spring.json.value.default.type=com.akhilpaila.payment.dto.OrderPlacedEvent"
        })
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed for orderId: {}",
            event.getOrderId());
        paymentService.processPayment(event);
    }
}
