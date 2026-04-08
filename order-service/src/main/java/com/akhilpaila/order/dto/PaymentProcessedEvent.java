package com.akhilpaila.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentProcessedEvent {
    private Long orderId;
    private String userId;
    private String status;
    private String idempotencyKey;
    private String failureReason;
}
