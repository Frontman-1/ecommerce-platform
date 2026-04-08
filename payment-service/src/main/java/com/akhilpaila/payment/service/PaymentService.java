package com.akhilpaila.payment.service;

import com.akhilpaila.payment.dto.OrderPlacedEvent;
import com.akhilpaila.payment.dto.PaymentResponse;
import java.util.List;

public interface PaymentService {
    void processPayment(OrderPlacedEvent event);
    PaymentResponse getPaymentByOrderId(Long orderId);
    List<PaymentResponse> getPaymentsByUserId(String userId);
}
