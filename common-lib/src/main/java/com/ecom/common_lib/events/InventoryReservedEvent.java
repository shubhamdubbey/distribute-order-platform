package com.ecom.common_lib.events;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {

    private UUID orderId;
    private UUID productId;
    private int quantity;
    private BigDecimal amount;
    private String correlationId;
    private Instant reservedAt;
}