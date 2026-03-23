package com.ecom.common_lib.events;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {

    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private UUID productId;
    private int quantity;
    private String correlationId;
    private Instant processedAt;
}