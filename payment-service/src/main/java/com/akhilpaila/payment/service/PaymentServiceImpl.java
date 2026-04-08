package com.akhilpaila.payment.service;

import com.akhilpaila.payment.dto.*;
import com.akhilpaila.payment.entity.Payment;
import com.akhilpaila.payment.kafka.PaymentEventPublisher;
import com.akhilpaila.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final StringRedisTemplate redisTemplate;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentEventPublisher paymentEventPublisher,
                              StringRedisTemplate redisTemplate) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void processPayment(OrderPlacedEvent event) {

        // Step 1: Check idempotency
        String redisKey = "payment:idempotency:"
            + event.getIdempotencyKey();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.warn("Payment already processed for key: {}",
                event.getIdempotencyKey());
            return;
        }

        // Step 2: Save payment with PROCESSING status
        Payment payment = Payment.builder()
            .orderId(event.getOrderId())
            .userId(event.getUserId())
            .amount(event.getTotalAmount())
            .status(Payment.PaymentStatus.PROCESSING)
            .idempotencyKey(event.getIdempotencyKey())
            .build();
        paymentRepository.save(payment);

        // Step 3: Simulate payment (90% success, 10% failure)
        boolean success = new Random().nextInt(10) != 0;

        if (success) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            log.info("Payment SUCCESS for orderId: {}",
                event.getOrderId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient funds");
            log.warn("Payment FAILED for orderId: {}",
                event.getOrderId());
        }
        paymentRepository.save(payment);

        // Step 4: Store idempotency key in Redis
        redisTemplate.opsForValue().set(redisKey, "PROCESSED",
            24, TimeUnit.HOURS);

        // Step 5: Publish payment.processed event
        PaymentProcessedEvent processedEvent = PaymentProcessedEvent
            .builder()
            .orderId(event.getOrderId())
            .userId(event.getUserId())
            .status(success ? "SUCCESS" : "FAILED")
            .idempotencyKey(event.getIdempotencyKey())
            .failureReason(success ? null : "Insufficient funds")
            .build();
        paymentEventPublisher.publishPaymentProcessed(processedEvent);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment not found for orderId: " + orderId));
        return mapToResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByUserId(String userId) {
        return paymentRepository.findByUserId(userId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .orderId(payment.getOrderId())
            .userId(payment.getUserId())
            .amount(payment.getAmount())
            .status(payment.getStatus())
            .failureReason(payment.getFailureReason())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}
