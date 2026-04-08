package com.akhilpaila.payment.dto;

import com.akhilpaila.payment.entity.Payment.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String userId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
}
