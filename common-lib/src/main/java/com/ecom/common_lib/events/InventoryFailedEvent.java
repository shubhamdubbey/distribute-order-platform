package com.ecom.common_lib.events;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedEvent {

    private UUID orderId;
    private UUID productId;
    private int quantity;
    private String reason;
    private String correlationId;
    private Instant failedAt;
}