package com.ecom.inventory_service.event;

import lombok.*;

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
    private String correlationId;
    private Instant reservedAt;
}