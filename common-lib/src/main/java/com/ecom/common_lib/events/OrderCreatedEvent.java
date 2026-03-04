package com.ecom.common_lib.events;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    private UUID orderId;
    private UUID userId;
    private UUID productId;
    private int quantity;
    private String status;
    private BigDecimal amount;
    private Instant createdAt;
}