package com.akhilpaila.payment.kafka;

import com.akhilpaila.payment.dto.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-processed}")
    private String paymentProcessedTopic;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Publishing payment.processed for orderId: {}",
            event.getOrderId());
        kafkaTemplate.send(paymentProcessedTopic,
            event.getOrderId().toString(), event);
    }
}
