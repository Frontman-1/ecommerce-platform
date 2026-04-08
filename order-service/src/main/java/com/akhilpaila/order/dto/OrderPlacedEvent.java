package com.akhilpaila.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPlacedEvent {
    private Long orderId;
    private String userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String idempotencyKey;
}
